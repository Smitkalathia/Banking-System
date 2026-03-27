Phase 5 Back End Unit Testing

Folder structure:
- backend/: copied Phase 4 Python back end files used by the tests
- tests/: white box unit tests and supplementary failure check
- results/: saved output from running the tests
- report/: Phase 5 report document

Run from the Phase5 folder:
python -m unittest discover -s tests -p "test_*.py" -v
python tests/phase5_failure_check.py
