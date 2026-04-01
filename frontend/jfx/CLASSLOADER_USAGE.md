# ClassLoader im Projekt verwenden

## Übersicht

Das Projekt verwendet `scala-reflect` für eine typsichere, macro-basierte Reflection-Schicht ohne JVM-Runtime-Reflection.

## Architektur

```
jfx.core.meta.Reflect (zentraler Einstieg)
├── ReflectClassLoader (global)
└── PackageClassLoader (pro Package)
    ├── domains
    ├── components
    ├── pages
    └── services
```

## Verwendung

### 1. Einfache Registrierung (global)

```scala
import jfx.core.meta.Reflect

// Domain-Klasse registrieren
val descriptor = Reflect.register(() => User("", ""))

// Klasse laden
val userClass = Reflect.loadClass[User]

// Instanz erstellen
val user = Reflect.createInstance[User]("app.domain.User")
```

### 2. Package-spezifische Registrierung

```scala
import jfx.core.meta.PackageClassLoader

val domainLoader = PackageClassLoader.domains

// Registrieren
domainLoader.register(() => Post("", "", 0))
domainLoader.register(() => Comment("", "", 0))

// Alle registrierten Klassen
val allClasses = domainLoader.getAllRegistered

// Subtypes finden
val entities = domainLoader.getSubTypes("app.domain.Entity")
```

### 3. In DomainRegistry

```scala
package app.domain

import jfx.core.meta.*

object DomainRegistry {
  
  def init(): Unit = {
    val loader = PackageClassLoader.domains
    
    // Alle Domain-Modelle registrieren
    loader.register(User.empty)
    loader.register(Post.empty)
    loader.register(Comment.empty)
  }
}
```

### 4. Mit existenziellen Typen

```scala
import jfx.core.meta.Reflect
import reflect.macros.ReflectMacros

// Für Table[?] oder andere parametrisierte Typen
val descriptor = ReflectMacros.reflect[Table[?]]
Reflect.classLoader.register[Table[?]](descriptor, () => Table[String]())
```

### 5. HATEOAS / Schema-Registrierung

```scala
import jfx.core.meta.Reflect
import app.support.Api

// API-Response-Typen registrieren
val loader = Reflect.classLoader

loader.register[Table[Data[User]]](
  ReflectMacros.reflect[Table[Data[User]]],
  () => Table(Data(User("test", "")))
)

loader.register[Data[User]](
  ReflectMacros.reflect[Data[User]],
  () => Data(User("test", ""), Nil)
)
```

## Wichtige Methoden

### Reflect (global)
- `Reflect.register(() => T)` – Registriert Typ global
- `Reflect.loadClass[T]` – Lädt ClassDescriptor
- `Reflect.createInstance[T](typeName)` – Erstellt Instanz
- `Reflect.getSubTypes[T]` – Findet Subtypen

### PackageClassLoader
- `PackageClassLoader.domains` – Loader für app.domain
- `PackageClassLoader.components` – Loader für app.components
- `loader.register(() => T)` – Registriert im Package
- `loader.getAllRegistered` – Alle registrierten Klassen
- `loader.getSubTypes(superType)` – Subtypen finden

## Best Practices

1. **Init in Main.scala**: DomainRegistry.init() in main() aufrufen
2. **Pro Feature-Modul**: Eigene Registry pro Package (domain, components, pages)
3. **Factory-Funktionen**: Companion Objects mit `empty` oder `apply` verwenden
4. **Typnamen**: `descriptor.typeName` für konsistente Namen verwenden
5. **Parent-Delegation**: PackageClassLoader erben vom globalen Loader

## Beispiel: Vollständige Registry

```scala
package app.domain

import jfx.core.meta.*

case class User(email: String, password: String)
object User {
  def empty: () => User = () => User("", "")
}

case class Post(title: String, content: String, authorId: Int)
object Post {
  def empty: () => Post = () => Post("", "", 0)
}

object DomainRegistry {
  def init(): Unit = {
    val loader = PackageClassLoader.domains
    loader.register(User.empty)
    loader.register(Post.empty)
  }
  
  def getUserType(): ClassDescriptor =
    loader.loadClass("app.domain.User").get
  
  def createUser(): User =
    loader.createInstance[User]("app.domain.User").get
}
```
