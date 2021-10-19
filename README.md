# Demo of Encoding a Fragmented MP4

To build a service that manually encodes a series of uncompress .wav media segments into a separate initialization **.mp4** and media segment **.m4s** for broadcast via [MPEG-DASH](https://en.wikipedia.org/wiki/Dynamic_Adaptive_Streaming_over_HTTP).

Uses [ffmpeg](https://www.ffmpeg.org/) to compress the .wav to .mp4
and [sannies/mp4parser](https://github.com/sannies/mp4parser) to mux the mp4 into .
