© 2026 NFRAC. All rights reserved.
This software is licensed under the NFRAC License. 
You may use, copy, modify, or distribute this software for personal or educational purposes only.
Commercial use is prohibited without prior written permission from NFRAC.

---

# Thank you for downloading the software.

Download this file and save it in the folder: C\Program Files\Squash

Structure as,
C:\Program Files\Squash\Squash.class
C:\Program Files\Squash\*


Add the Squash.bash path to env variables

Open a terminal and use the following commands to compress and decompress files.

## Compress a file or folder
./squash -compress origin_file target_file_name

Example:
./squash -compress "E:\\Folder\\Folder" ./arc.tar.sq

## Decompress a squash archive
./squash -decompress target_file_name.tar.sq

Example:
./squash -decompress ./arc.tar.sq

---

Please ensure that you open the terminal within the `squash-compressor` directory before executing the commands.
