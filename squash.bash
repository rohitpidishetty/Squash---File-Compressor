#!/bin/bash

# ===================================================
# NFRAC Squash Script for macOS / Linux
# ===================================================

# Colors
YELLOW='\033[1;33m'
BLUE='\033[1;34m'
NC='\033[0m' # No Color

# Print ASCII Art
echo
echo -e "${YELLOW}  ######    ######    ##    ##    ###     ######  ##   ## ${NC}"
echo -e "${YELLOW} ##        ##    ##   ##    ##   ## ##   ##       ##   ## ${NC}"
echo -e "${YELLOW}  #####    ## ## ##   ##    ##  #######   #####   ####### ${NC}"
echo -e "${YELLOW}      ##   ##   ##    ##    ##  ##   ##       ##  ##   ## ${NC}"
echo -e "${YELLOW} ######     #### ##    ######   ##   ##  ######   ##   ## ${NC}"
echo
echo -e "${BLUE}          All @copyrights reserved by NFRAC${NC}"
echo

# Check if argument is provided
if [ -z "$1" ]; then
  echo "Usage: squash -compress origin_path target_file_name"
  echo "Usage: squash -decompress target_file_name.tar.sq"
  exit 1
fi

# Compress
if [ "$1" == "-compress" ]; then
  if [ -z "$2" ] || [ -z "$3" ]; then
    echo "Usage: squash -compress origin_path target_file_name"
    exit 1
  fi
  java Squash -compress "$2" "$3"
  exit 0
fi

# Decompress
if [ "$1" == "-decompress" ]; then
  if [ -z "$2" ]; then
    echo "Usage: squash -decompress target_file_name.tar.sq"
    exit 1
  fi
  java Squash -decompress "$2"
  exit 0
fi

# If invalid argument
echo "Usage: squash -compress origin_path target_file_name"
echo "Usage: squash -decompress target_file_name.tar.sq"
exit 1
