package jfx.test

import jfx.core.state.{ListProperty, Property}
import jfx.form.Model
import reflect.ReflectRegistry

class SimpleModel extends Model[SimpleModel] {
  val name: Property[String] = Property("")
  val value: Property[Int] = Property(0)
}

class NestedModel extends Model[NestedModel] {
  val title: Property[String] = Property("")
  val count: Property[Int] = Property(0)
}

class ParentModel extends Model[ParentModel] {
  val name: Property[String] = Property("")
  val nested: Property[NestedModel] = Property(new NestedModel)
}

class ListModel extends Model[ListModel] {
  val name: Property[String] = Property("")
  val items: ListProperty[String] = new ListProperty[String]()
}

class BooleanModel extends Model[BooleanModel] {
  val name: Property[String] = Property("")
  val active: Property[Boolean] = Property(false)
}

class DoubleModel extends Model[DoubleModel] {
  val name: Property[String] = Property("")
  val amount: Property[Double] = Property(0.0)
}

class OptionModel extends Model[OptionModel] {
  val name: Property[String] = Property("")
  val description: Property[Option[String]] = Property(None)
}

class MapModel extends Model[MapModel] {
  val name: Property[String] = Property("")
  val metadata: Property[Map[String, String]] = Property(Map.empty[String, String])
}

object TestModelRegistry {
  println("=== Registering Test Models ===")
  val simpleDesc = ReflectRegistry.register(() => new SimpleModel())
  println(s"Registered SimpleModel with typeName: ${simpleDesc.typeName}")
  
  ReflectRegistry.register(() => new NestedModel())
  ReflectRegistry.register(() => new ParentModel())
  ReflectRegistry.register(() => new ListModel())
  ReflectRegistry.register(() => new BooleanModel())
  ReflectRegistry.register(() => new DoubleModel())
  ReflectRegistry.register(() => new OptionModel())
  ReflectRegistry.register(() => new MapModel())
  println("=== Done Registering ===")
}
