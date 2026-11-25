#!/usr/bin/env bash
# SPDX-License-Identifier: GPL-3.0-or-later
# Copyright (C) 2026 Ayuless contributors
# Run the same fast, SDK-independent checks locally and on pull requests.
set -euo pipefail
cd "$(dirname "$0")/../.."

python3 Tools/localization/sync_translations.py
python3 -m unittest discover -s Tools/localization -p 'test_*.py'
python3 Tools/test_strings_build.py
python3 Tools/test_registration_estimate.py
python3 Tools/check_degoogle.py
