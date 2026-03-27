"""
print_error.py

Provides helper functions for reporting errors in the Banking System Back End.
"""


def print_error(error_type, message):
    """
    Print an error message in a standard format.

    Parameters:
        error_type (str): Either 'error' or 'fatal'.
        message (str): The message to display.
    """

    error_type = error_type.lower().strip()

    if error_type == "fatal":
        print(f"FATAL ERROR: {message}")
        exit(1)

    elif error_type == "error":
        print(f"ERROR: {message}")

    else:
        print(f"UNKNOWN ERROR TYPE: {message}")