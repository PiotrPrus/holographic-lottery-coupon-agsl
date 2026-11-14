# Scratchify (vendored copy)

Source: https://github.com/gsrathoreniks/Scratchify (`scratchify/src/commonMain`, commit a85c725), MIT License, Copyright (c) 2025 Gajendra Singh Rathore. See `LICENSE`.

The published artifact (`io.github.gsrathoreniks:scratchify:1.0.0-alpha2`) has Android and iOS targets only, so the desktop deck can't depend on it. These files are an unchanged copy of the common code, so the talk can run the library live. One change: `createHapticFeedbackManager()` is a plain function that returns the no-op manager instead of an `expect` declaration.
