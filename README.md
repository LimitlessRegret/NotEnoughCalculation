# Not Enough Calculation

## Prerequisites

* JavaFX 8 installation

    * If you're getting `ClassNotFoundException`s regarding `nec.gui.NecApp` for seemingly no reason, this is likely the cause.

    * Azul provides JDK builds including JavaFX:
      https://www.azul.com/downloads/zulu-community/?version=java-8-lts&architecture=x86-64-bit&package=jdk-fx

* A recipe database (`nec.db`)

    * A prebuilt GTNH 2.0.9.0 database is available under Releases

    * A flow for building the database from a `RecEx` dump is pending, the code is present however.

* Microsoft Visual C++ Redistributable for Visual Studio 2015, 2017 and 2019 (Windows users)

    `x64: vc_redist.x64.exe` from https://support.microsoft.com/en-us/help/2977003/the-latest-supported-visual-c-downloads
    
    * This is a dependency of `or-tools`
