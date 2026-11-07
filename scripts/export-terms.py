#!/usr/bin/env python3
"""Bundle the five canonical terms documents for DotSuite's public HTML page."""
import argparse
import json
from pathlib import Path

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument("--output", required=True, type=Path)
args = parser.parse_args()
root = Path(__file__).resolve().parents[1]
documents = {}
for language in ("en", "ar", "de", "fr", "ru"):
    text = (root / "docs" / "legal" / f"terms.{language}.md").read_text()
    if sum(line.startswith("## ") for line in text.splitlines()) != 11:
        raise ValueError(f"Expected eleven sections for {language}")
    if "support@dotsuite.dev" not in text or "#delete-dotshredzilla" not in text:
        raise ValueError(f"Missing support/deletion link for {language}")
    documents[language] = text
args.output.parent.mkdir(parents=True, exist_ok=True)
args.output.write_text(json.dumps(documents, ensure_ascii=False, indent=2) + "\n")
print(f"Exported {len(documents)} complete documents to {args.output}")
