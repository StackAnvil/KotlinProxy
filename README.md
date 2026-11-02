# KotlinProxy

KotlinProxy is a standalone Minecraft proxy that lets players join servers running
almost any Minecraft version, from Classic to the latest release. It is a Kotlin
rewrite of [ViaProxy](https://github.com/ViaVersion/ViaProxy) and uses
[SpongePowered Mixin](https://github.com/SpongePowered/Mixin) for runtime patches.

## Features

- Connects clients and servers on different Minecraft versions
- Supports Java Edition, Bedrock Edition and legacy Classic servers
- Runs as a GUI, a CLI, or from a YAML configuration file
- Loads plugins from the `plugins` directory
- Works with servers in offline mode
- Ships a Java 8 compatible build

KotlinProxy is a protocol proxy. It does not modify the client or the server.
It applies its patches with the SpongePowered Mixin runtime from a child-first
class loader, so no `-javaagent` flag or launcher wrapper is required.

## Requirements

- Java 17 or newer to run the regular build
- Java 8 or newer to run the `+java8` build
- A writable working directory

## Usage

```text
java -jar KotlinProxy.jar
java -jar KotlinProxy.jar config <config file>
java -jar KotlinProxy.jar cli --help
```

A small CLI session that forwards a 1.8 client to a 26.3 server:

```text
java -jar KotlinProxy.jar cli \
  --bind-address 0.0.0.0:25568 \
  --target-address 127.0.0.1:25565 \
  --target-version 26.3 \
  --auth-method NONE \
  --proxy-online-mode false
```

Set `target-version` to `Auto Detect (1.7+ servers)` to let the proxy detect the
server version.

## Plugins

Plugins are JAR files in the `plugins` folder. Each plugin contains a
`viaproxy.yml` with `name`, `author`, `version`, `main`, and optional `depends`.
Plugin classes can listen to the events in
`net.raphimc.viaproxy.plugins.events`.

Runtime patches use SpongePowered Mixin. A plugin that needs to patch proxy or
ViaVersion classes can ship a standard mixin configuration and register it from
its plugin entry point.

## Building

```text
./gradlew build
```

The build produces `build/libs/KotlinProxy-<version>.jar` and
`build/libs/KotlinProxy-<version>+java8.jar`, a variant downgraded to Java 8
bytecode. Both variants apply the same mixins.

## Differences from ViaProxy

- All source files are Kotlin.
- Runtime patches are applied by the SpongePowered Mixin runtime instead of
  ClassTransform.
- The plugin loader uses plain URL class loaders. Plugins that relied on
  ClassTransform transformers must provide a Mixin configuration instead.

## License

KotlinProxy is licensed under the GNU General Public License v3.0. It is derived
from ViaProxy, copyright RK_01/RaphiMC and contributors.
