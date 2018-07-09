# Package com.github.cheapmon.apc

`apc` extracts privacy policy texts or xml models from any Android app.

Main pipeline:
* Read list of bundle ids from command line or input file
* Install special `.apk` on an Android (virtual) device
* Send configuration to and run `àpc` test application on device
* Wait for text or model and save to separate file per id

For further information on how the extraction works, see the documentation
of the `droid` submodule.