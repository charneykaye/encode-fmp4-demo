# Demo of Encoding a Fragmented MP4

To build a service that manually encodes a series of uncompressed .wav media segments into a separate initialization **.mp4** and media segment **.m4s** for broadcast via [MPEG-DASH](https://en.wikipedia.org/wiki/Dynamic_Adaptive_Streaming_over_HTTP).

I'm using [ffmpeg](https://www.ffmpeg.org/) to compress the .wav to .aac
and [sannies/mp4parser](https://github.com/sannies/mp4parser) to mux the aac audio into an **.m4s** media fragment.

Here's my custom [ChunkMp4Builder](src/main/java/com/charneykaye/ChunkMp4Builder.java) class.

Here's a .yaml representation of the **.m4s** media fragment I'm trying to create:

```YAML
SegmentTypeBox:
  majorBrand: msdh
  minorVersion: 0
  compatibleBrand:
    - msdh
    - msix

SegmentIndexBox:
  entries:
    - Entry:
        referenceType: 0
        referencedSize: 64690
        subsegmentDuration: 480000
        startsWithSap: 1
        sapType: 0
        sapDeltaTime: 0
  referenceId: 1
  timeScale: 48000
  earliestPresentationTime: 0
  firstOffset: 0
  reserved: 0

MovieFragmentBox:
  MovieFragmentHeaderBox:
    sequenceNumber: 1

  TrackFragmentBox:
    TrackFragmentHeaderBox:
      trackId: 1
      baseDataOffset: -1
      sampleDescriptionIndex: 0
      defaultSampleDuration: 1024
      defaultSampleSize: 111
      defaultSampleFlags:
        - SampleFlags:
            reserved: 0
            isLeading: 0
            depOn: 2
            isDepOn: 0
            hasRedundancy: 0
            padValue: 0
            isDiffSample: false
            degradPrio: 0
      durationIsEmpty: false
      defaultBaseIsMoof: true

    TrackFragmentBaseMediaDecodeTimeBox:
      baseMediaDecodeTime: 0

    TrackRunBox:
      sampleCount: 470
      dataOffset: 3868
      dataOffsetPresent: true
      sampleSizePresent: true
      sampleDurationPresent: true
      sampleFlagsPresentPresent: false
      sampleCompositionTimeOffsetPresent: false
      firstSampleFlags: null
```

My implementation creates an MP4 without error. But when I try to use IsoFile to read it back, I see:

```
A cast to int has gone wrong. Please contact the mp4parser discussion group (3724673092)
```

I've created this question on Stack Overflow:

https://stackoverflow.com/questions/69625970/java-mp4parser-to-create-m4s-fragment