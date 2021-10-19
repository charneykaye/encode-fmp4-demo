https://stackoverflow.com/questions/69625970/java-mp4parser-to-create-m4s-fragment

[![Production CI](https://github.com/charneykaye/encode-fmp4-demo/actions/workflows/main.yml/badge.svg?branch=main)](https://github.com/charneykaye/encode-fmp4-demo/actions/workflows/main.yml)

# Demo of Encoding a Fragmented MP4

To build a service that manually encodes a series of uncompressed .wav media segments into a separate initialization **.mp4** and media segment **.m4s** for broadcast via [MPEG-DASH](https://en.wikipedia.org/wiki/Dynamic_Adaptive_Streaming_over_HTTP), using [ffmpeg](https://www.ffmpeg.org/) to compress the .wav to .aac
and [sannies/mp4parser](https://github.com/sannies/mp4parser) to mux the aac audio into an **.m4s** media fragment.

I created this public GitHub project to reproduce the issue in its entirety.

For example, here's the custom [CustomFragmentMp4Builder.java](src/main/java/com/charneykaye/CustomFragmentMp4Builder.java) class.

The objective is to build an **.m4s** fragment comprising the box types `SegmentTypeBox`, `SegmentIndexBox`, and `MovieFragmentBox`. As For reference, I have used *mp4parser* to inspect an **.m4s** fragment that was generated via `ffmpeg -f hls`. This specification is available [here as a .yaml file](src/test/resources/test5-128k-151304042-ffmpeg.yaml)

My implementation creates an MP4 without error. But when I try to use IsoFile to read it back, I see:

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

But, the stack traced error above happens when the unit test attempted to read the file that the ChunkMp4Builder just wrote to a temp folder. There seems to be a box of an unknown type, appearing at the end of the file.

[![There seems to be a box of an unknown type, appearing at the end of the file.][3]][3]

[2]: https://i.stack.imgur.com/aAmyt.png
[3]: https://i.stack.imgur.com/pHJeJ.png

