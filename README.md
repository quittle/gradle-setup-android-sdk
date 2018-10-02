# Development

See `.travis.yml` for how CI/CD performs builds.

In general, perform builds in the context of each folder, rather than as a multi-project Gradle
build. This is necessary because the `example-android-project` will fail to configure without the
plugin being locally available so the `android-sdk-installer` project must be built and deployed
locally first.

In general, to build locally.
```
$ ./gradlew -p android-sdk-installer # This runs all the default tasks
$ ./gradlew -p android-sdk-installer publishToMavenLocal
$ ./gradlew -p example-android-project assemble
```