"""Guards the exact-pin policy: the environment must match requirements.txt, and the pinned
packages must satisfy each other's *declared* requirements (notably FastAPI -> Starlette)."""

from importlib.metadata import PackageNotFoundError, requires, version
from pathlib import Path

from packaging.requirements import Requirement
from packaging.utils import canonicalize_name

REQUIREMENTS = Path(__file__).resolve().parent.parent / "requirements.txt"


def _pins() -> dict[str, str]:
    pins: dict[str, str] = {}
    for raw in REQUIREMENTS.read_text().splitlines():
        line = raw.strip()
        if not line or line.startswith("#"):
            continue
        requirement = Requirement(line)
        specs = list(requirement.specifier)
        assert len(specs) == 1 and specs[0].operator == "==", f"not an exact pin: {line}"
        pins[canonicalize_name(requirement.name)] = specs[0].version
    return pins


def test_installed_versions_match_requirements_txt():
    mismatches = []
    for name, pinned in _pins().items():
        try:
            installed = version(name)
        except PackageNotFoundError:
            mismatches.append(f"{name}: pinned {pinned}, not installed")
            continue
        if installed != pinned:
            mismatches.append(f"{name}: pinned {pinned}, installed {installed}")
    assert not mismatches, "environment drifted from requirements.txt: " + "; ".join(mismatches)


def test_fastapi_declared_requirements_are_satisfied_by_the_pins():
    """FastAPI declares a version range for Starlette (and Pydantic); the pins must be inside it."""
    violations = []
    for raw in requires("fastapi") or []:
        requirement = Requirement(raw)
        if requirement.marker is not None and "extra" in str(requirement.marker):
            continue  # optional extras are not part of the runtime contract
        try:
            installed = version(requirement.name)
        except PackageNotFoundError:
            violations.append(f"{requirement.name} required by fastapi but not installed")
            continue
        if not requirement.specifier.contains(installed, prereleases=True):
            violations.append(f"{requirement.name} {installed} not in '{requirement.specifier}'")
    assert not violations, "; ".join(violations)


def test_every_runtime_pin_is_exact():
    assert _pins(), "requirements.txt must pin the runtime packages"
