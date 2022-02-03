#!/usr/bin/env bash
#
# Simple script to automate updating the Action without changing the Release Tag
#
echo Touch >> trigger.file
git commit -a -m "$1"
git push -u origin main
