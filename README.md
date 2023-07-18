# Artify

This is a web platform designed for and made by artists. The main goals are to create a community in which sharing your
art no matter your experience is encouraged and rewarded. Promoting commissions and lastly, creating a lively portfolio
that can be used in job hunting.

## Modules

The platform is built up from several modules that have different responsibilities in order to stay as scalable
and future-proof as possible.

* Core - Contains shared objects and functionality, mainly serializable objects as well as other utility.
* [Api](api/README.md) - Contains all the public facing api endpoints.
* [Image processor](image-processor/README.md) - Processes uploaded images, crops and compresses them.

## Dependency management

Dependency management within this project is done through 
[Gradle's version catalogs](https://docs.gradle.org/current/userguide/platforms.html) system.
All dependencies and their versions can be found in [this](gradle/libs.versions.toml) version catalog. 
Any plugins or dependencies or updates must be reflected within this version catalog.
Furthermore, [Dependabot](https://github.com/dependabot/dependabot-core) is used to scan for out-of-date dependencies
on a weekly basis and pull requests are automatically created to update these dependencies and merged into the staging
branch.
