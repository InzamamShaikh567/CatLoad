<div align="center">

<img src="src/main/resources/icons/tabby.png" width="128" alt="CatLoad icon" />

# CatLoad

A free and open-source cross-platform media downloader built with JavaFX 21. Designed to be lightweight, fast, and memory-efficient while supporting videos, audio, and entire playlists.

Supports YouTube, YouTube Shorts, TikTok, Instagram, Reddit and [thousands of other sites](https://github.com/yt-dlp/yt-dlp/blob/master/supportedsites.md) via yt-dlp.

Everything runs locally on your machine - no cloud, no login, no accounts, no tracking and support for Cookies.txt.

</div>

<br/>

<p align="center">
  <img src="screenshot/gif/theme.gif" width="80%" alt="CatLoad theme switching" />
</p>


## ⬇️ Download for Windows

<a href="https://github.com/InzamamShaikh567/CatLoad/releases/latest/download/CatLoad-1.0.0.exe">
  <img src="https://img.shields.io/badge/Download-Windows%20EXE-blue?style=for-the-badge&logo=windows" />
</a>

<a href="https://github.com/InzamamShaikh567/CatLoad/releases/latest/download/CatLoad-1.0.0.msi">
  <img src="https://img.shields.io/badge/Download-Windows%20MSI-blue?style=for-the-badge&logo=windows" />
</a>

<a href="https://github.com/InzamamShaikh567/CatLoad/releases/latest/download/CatLoad-Portable.zip">
  <img src="https://img.shields.io/badge/Download-Portable%20ZIP-lightgrey?style=for-the-badge&logo=windows" />
</a>



---

## 🐧 Download for Linux

<a href="https://github.com/InzamamShaikh567/CatLoad/releases/latest/download/catload_1.0.0_amd64.deb">
  <img src="https://img.shields.io/badge/Download-DEB-orange?style=for-the-badge&logo=debian" />
</a>

<a href="https://github.com/InzamamShaikh567/CatLoad/releases/latest/download/catload-1.0.0-1.x86_64.rpm">
  <img src="https://img.shields.io/badge/Download-RPM-red?style=for-the-badge&logo=fedora" />
</a>

<a href="https://github.com/InzamamShaikh567/CatLoad/releases/latest/download/CatLoad-1.0.0-x86_64.AppImage">
  <img src="https://img.shields.io/badge/Download-AppImage-green?style=for-the-badge&logo=linux" />
</a>

## Features

- Custom video/audio stream selection
- Playlist downloading
- Number prefixes for playlists
- Playlist folders
- Cookie support (cookies.txt)
- Concurrent downloads
- Retry and resume downloads
- Automatic yt-dlp updates
- Automatic FFmpeg installation
- Windows & Linux support

### Universal Download
Paste any URL and CatLoad handles the rest.

<p align="center">
  <img src="screenshot/normaldownload.png" width="80%" alt="Normal download" />
</p>

### Custom Stream Selection
Pick specific video and audio formats before downloading.

<p align="center">
  <img src="screenshot/customdownload.png" width="80%" alt="Custom download options" />
</p>

<p align="center">
  <img src="screenshot/gif/customdownload.gif" width="80%" alt="Custom download options" />
</p>

### Cookie Support
Import cookies.txt for restricted or account-linked content.

Learn how to get your [cookies.txt](https://www.reddit.com/r/youtubedl/wiki/cookies) file and load it into the app settings. 

Note: When downloading a large number of videos, using a VPN may help reduce the chance of IP rate limiting.

<p align="center">
  <img src="screenshot/cookies.png" width="80%" alt="Cookie import" />
</p>


### Playlist Link Support 
Optional settings let you automatically add number prefixes and organize downloads into playlist folders.
<p align="center">
  <img src="screenshot/gif/playlistLink.gif" width="80%" alt="Playlist link" />
</p>



### Number Prefix & Playlist Folders 
"Add number prefixes" Automatically automatically renames downloaded files based on their playlist order.
"Save to playlist folder" Saves downloaded videos inside a folder named after the playlist.


<p align="center">
  <img src="screenshot/gif/Number-Prefix, thumbnail-playlist-folder.gif" width="80%" alt="Number prefix and playlist folder" />
</p>


<p align="center">
  <img src="screenshot/gif/playlistomages.gif" width="80%" alt="Playlist browsing" />
</p>



### Retry & Resume
Automatically retries failed downloads with support for cancellation.

<p align="center">
  <img src="screenshot/gif/failed_downloading_complete.gif" width="80%" alt="Failed download handling" />
</p>

### Auto Engine Setup
Update yt-dlp and install FFmpeg directly from the application.

<p align="center">
  <img src="screenshot/ytdlp-ffmpeg-engine.png" width="80%" alt="Engine management" />
</p>

### Concurrent Downloads
Supports configurable parallel downloads (up to 10 at once) with a download queue for efficient processing.

<p align="center">
  <img src="screenshot/concurrentdownload.png" width="80%" alt="Concurrent downloads" />
</p>

### Support CatLoad

If CatLoad has been useful to you, consider supporting its development. 

<a href="https://www.paypal.com/paypalme/GaffarAliShaikh">
  <img src="screenshot/support.svg" alt="Support CaLoad" />
</a>

## Tech Stack

- **Java 21** with JavaFX 21
- **yt-dlp** download engine
- **FFmpeg** stream merging
- **OkHttp 4** networking
- **Jackson** JSON serialization
- **Maven** build system



## License

This project is licensed under the GNU Affero General Public License v3.0 (AGPL-3.0).

See the [LICENSE](https://github.com/InzamamShaikh567/CatLoad/blob/main/LICENSE) file for the full license text.
