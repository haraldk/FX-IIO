JavaFX ImageIO bridge
=====================

Allows reading/writing JavaFX `WritableImage/Image`s using the `javax.imageio` package, for greatly extended format support in JavaFX.

This solution reads or writes directly from/to the JavaFX `Image/WritableImage`, and does not simply use `SwingFXUtils.toFXImage` after the image was read, making it a lot more memory-efficient and possibly faster, especially for reading large images.
