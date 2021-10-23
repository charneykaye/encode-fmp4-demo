https://stackoverflow.com/questions/69625970/java-mp4parser-to-create-m4s-fragment

[![Production CI](https://github.com/charneykaye/encode-fmp4-demo/actions/workflows/main.yml/badge.svg?branch=main)](https://github.com/charneykaye/encode-fmp4-demo/actions/workflows/main.yml)

## Comparing methods of MP4 creation

The first example using MP4 box works, insofar as I am able to generate an initializing MP4 + series of fragment M4s
files which can then be concatenated to form a playable MPEG4 stream.

Note: it's a requirement for this use case that each media segment be encoded from individually generated source
segments, versus using a tool such as MP4Box to stream from a continuous audio source.

## via Java mp4parser (malformed)

**Attempts to manually build media segments via mp4parser are still failing overall,** because the fragments written by
my [CustomFragmentMp4Builder.java](src/main/java/com/charneykaye/CustomFragmentMp4Builder.java) used below are
malformed. But I'm having a difficult time understanding *how* exactly they are malformed.

```java
Files.deleteIfExists(Path.of(m4sFilePath));
AACTrackImpl aacTrack=new AACTrackImpl(new FileDataSourceImpl(aacFilePath));
Movie movie=new Movie();
movie.addTrack(aacTrack);
Container mp4file=new CustomFragmentMp4Builder(hz,seconds,seqNum,bufferSize).build(movie);
FileChannel fc=new FileOutputStream(m4sFilePath).getChannel();
mp4file.writeContainer(fc);
fc.close();
```

- [test5.mpd](notes/via-java-mp4parser/test5.mpd)
- [test5-128k-163493804.m4s](notes/via-java-mp4parser/test5-128k-163493804.m4s)
- [test5-128k-163493805.m4s](notes/via-java-mp4parser/test5-128k-163493805.m4s)
- [test5-128k-163493806.m4s](notes/via-java-mp4parser/test5-128k-163493806.m4s)
- [test5-128k-IS.mp4](notes/via-java-mp4parser/test5-128k-IS.mp4)

## via MP4Box (ok)

```shell
MP4Box \
  -profile live \
  -add aacFilePath \
  -dash 10000 \
  -frag 10000 \
  -idx ${NUM} \
  -moof-sn ${NUM} \
  -out test5.mpd \
  -segment-name test5-128k- \
  -segment-ext m4s \
  -single-traf \
  -subsegs-per-sidx 0 \
  -daisy-chain \
  -single-segment \
  /tmp
```

- [test5.mpd](notes/via-mp4box/test5.mpd)
- [test5-128k-163494320.m4s](notes/via-mp4box/test5-128k-163494320.m4s)
- [test5-128k-163494321.m4s](notes/via-mp4box/test5-128k-163494321.m4s)
- [test5-128k-163494322.m4s](notes/via-mp4box/test5-128k-163494322.m4s)
- [test5-128k-IS.mp4](notes/via-mp4box/test5-128k-IS.mp4)

## Demo of Encoding a Fragmented MP4

This use case is a service that manually encodes a series of uncompressed .wav media segments into **.m4s** fragments
for broadcast via [MPEG-DASH](https://en.wikipedia.org/wiki/Dynamic_Adaptive_Streaming_over_HTTP),
using [ffmpeg](https://www.ffmpeg.org/) to compress the .wav to .aac
and [sannies/mp4parser](https://github.com/sannies/mp4parser) to assemble the aac audio into an **.m4s** media fragment.

I created this public GitHub project to reproduce the issue in its entirety.

For example, here's the
custom [CustomFragmentMp4Builder.java](src/main/java/com/charneykaye/CustomFragmentMp4Builder.java) class.

The objective is to build an **.m4s** fragment comprising the box types `SegmentTypeBox`, `SegmentIndexBox`,
and `MovieFragmentBox`. As For reference, I have used *mp4parser* to inspect an **.m4s** fragment that was generated
via `ffmpeg -f hls`. This specification is
available [here as a .yaml file](src/test/resources/test5-128k-151304042-ffmpeg.yaml)

My implementation creates an MP4 without error. But, when the unit test attempts to read the file that the
ChunkMp4Builder just wrote to a temp folder:

```
java.lang.RuntimeException: A cast to int has gone wrong. Please contact the mp4parser discussion group (3724673092)
	at org.mp4parser.tools.CastUtils.l2i(CastUtils.java:30)
	at org.mp4parser.support.AbstractBox.parse(AbstractBox.java:97)
	at org.mp4parser.AbstractBoxParser.parseBox(AbstractBoxParser.java:116)
	at org.mp4parser.BasicContainer.initContainer(BasicContainer.java:107)
	at org.mp4parser.IsoFile.<init>(IsoFile.java:57)
	at org.mp4parser.IsoFile.<init>(IsoFile.java:52)
	at com.charneykaye.TestBase.getMp4Boxes(TestBase.java:116)
	at com.charneykaye.CustomFragmentMp4BuilderTest.run(CustomFragmentMp4BuilderTest.java:78)
```

The expected box types `SegmentTypeBox`, `SegmentIndexBox`, and `MovieFragmentBox` do appear in the output:

[![The expected box types `SegmentTypeBox`, `SegmentIndexBox`, and `MovieFragmentBox` do appear in the output][2]][2]

However, at the end of the file appears a box of an unknown type:

[![There seems to be a box of an unknown type, appearing at the end of the file.][3]][3]




## Fragmented MP4 has moof, not moov

Solved by [@aergistal](https://stackoverflow.com/users/4663670/aergistal) on Stack Overflow
https://stackoverflow.com/a/69668111/1335245

See diagram of Fragmented MP4 (fmp4): https://bitmovin.com/wp-content/uploads/2019/07/image7.png

Your `m4s` segments are invalid due to an incorrect `mdat` atom size.

For example in `test5-128k-151304042.m4s` the `mdat` is marked as having a length of 16 bytes but there is data at the end and file size is 164884.

The parser then attempts to read an invalid offset. `avc5` is not an atom but actually part of the string "Lavc58.54.100". The length read as 3724673100 is also invalid and greater than the max for a 32-bit integer, hence the invalid cast to int.

[![hex dump][1]][1]


----------

In your implementation you have:

    ParsableBox moov = createMovieFragmentBox(movie);
    isoFile.addBox(moov);
    List<SampleSizeBox> stszs = Path.getPaths(moov, "trak/mdia/minf/stbl/stsz");
    // ...

    protected MovieFragmentBox createMovieFragmentBox(Movie movie) {
        MovieFragmentBox mfb = new MovieFragmentBox();
        // ...
    }

This is not a `moov` atom, it's a `moof`. There is no `stsz` in there and the sum of your sample sizes is 0 so the total calculated size of the `mdat` is 16 + 0.

The `moov` is supposed to be in the initialization segment.


  [1]: https://i.stack.imgur.com/4z7gE.jpg


[2]: https://i.stack.imgur.com/aAmyt.png

[3]: https://i.stack.imgur.com/pHJeJ.png
