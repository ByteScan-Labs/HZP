# HZP — Hardened ZIP Parser

[![](https://jitpack.io/v/ByteScan-Labs/HZP.svg)](https://jitpack.io/#ByteScan-Labs/HZP)

A Java library for parsing ZIP archives with structural anomalies or evasion techniques commonly used to bypass security tools.

This source was primarily made available for transparency at ByteScan, but feel free to use it as long as it complies with our license.

> HZP's naming conventions were inspired by [LL-Java-ZIP](https://github.com/Col-E/LL-Java-Zip).
> No code or functionality was derived from their project.

> This project is by no means perfect. If you find any issues, feel free to open an issue, and you'll be credited for reporting them.

## Features

- Detects evasion techniques including fake EOCDS, prepended data, encrypted entries, duplicate entry names, and more.
- Resolves central-directory offset anomalies and shuffled entry order.
- Full ZIP32 and ZIP64 support.

## Usage

```java
final ZipArchive archive = ZipIO.read(new File("sample.zip"));

for(final LocalFileHeader header : archive.getLocalFiles()) {
    final String name = header.getName();
    final byte[] data = header.decompress();
}

// Look up a specific entry, and decompress it.
archive.getEntry("config/settings.json").ifPresent(cd -> {
    final byte[] data = cd.getLinkedLFH().decompress();
});

final List<CentralDirectoryFileHeader> cds = archive.getCentralDirectories();

// Information about the archive.
final EndOfCentralDirectory end = archive.getEnd();
```

## License

Apache 2.0 with Commons Clause. Commercial use, resale, or offering this library as a hosted service requires a separate agreement.

[You can view the full license here](https://raw.githubusercontent.com/ByteScan-Labs/HZP/refs/heads/main/LICENSE).