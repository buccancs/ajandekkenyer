"""Stub for colorlog to bypass installation issues during development."""
import logging

def ColoredFormatter(*args, **kwargs):
    """Simple fallback for ColoredFormatter."""
    return logging.Formatter()

# Add to logging module to mimic colorlog
logging.ColoredFormatter = ColoredFormatter