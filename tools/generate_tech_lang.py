#!/usr/bin/env python3
"""Compatibility entry point for the strict pipeline; guessing translations is retired.

Use --write to regenerate from the pinned original and explicit aliases.
"""
from localization import main

if __name__ == '__main__':
    main()
