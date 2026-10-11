"""Compatibility API: all Chinese values now come from the pinned original source."""
from localization import main, synchronize


def sync():
    return synchronize(write=True)


if __name__ == '__main__':
    main()
