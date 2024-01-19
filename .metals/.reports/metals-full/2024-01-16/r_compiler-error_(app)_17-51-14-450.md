file://<WORKSPACE>/app/src/main/scala/com/rockthejvm/jobsboard/App.scala
### java.lang.AssertionError: NoDenotation.owner

occurred in the presentation compiler.

action parameters:
offset: 643
uri: file://<WORKSPACE>/app/src/main/scala/com/rockthejvm/jobsboard/App.scala
text:
```scala
package com.rockthejvm.jobsboard

import tyrian.*
import tyrian.Html.*
import cats.effect.*
import scala.scalajs.js.annotation.*
import org.scalajs.dom.document
import cats.effect.IO

@JSExportTopLevel("RockTheJvmApp")
class App extends TyrianApp[Int, String] {
  //                        ^^ Message  ^^ model="state"

  /*
    We can send Messages by
    - trigger a command
    - create a subscription
   */
  override def init(flags: Map[String, String]): (String, Cmd[IO, Int]) =
    ("", Cmd.None)

  //  potentially endless stream of messages (here of type Int)
  override def subscriptions(model: String): Sub[IO, Int] =
    Sub.every[@@]

  //  model can change by receiving messages
  //  model => message => (new model, new command)
  //  update triggered whenever we get a new message
  override def update(model: String): Int => (String, Cmd[IO, Int]) =
    message => (model + ", " + message, Cmd.None)

  //  view triggered whenever model changes
  override def view(
      model: String
  ): Html[Int] = //  triggered every time model is changed. Model is changes via messages
    div(s"Tyrian running: $model")
}

```



#### Error stacktrace:

```
dotty.tools.dotc.core.SymDenotations$NoDenotation$.owner(SymDenotations.scala:2582)
	scala.meta.internal.pc.SignatureHelpProvider$.isValid(SignatureHelpProvider.scala:83)
	scala.meta.internal.pc.SignatureHelpProvider$.notCurrentApply(SignatureHelpProvider.scala:96)
	scala.meta.internal.pc.SignatureHelpProvider$.$anonfun$1(SignatureHelpProvider.scala:48)
	scala.collection.StrictOptimizedLinearSeqOps.loop$3(LinearSeq.scala:280)
	scala.collection.StrictOptimizedLinearSeqOps.dropWhile(LinearSeq.scala:282)
	scala.collection.StrictOptimizedLinearSeqOps.dropWhile$(LinearSeq.scala:278)
	scala.collection.immutable.List.dropWhile(List.scala:79)
	scala.meta.internal.pc.SignatureHelpProvider$.signatureHelp(SignatureHelpProvider.scala:48)
	scala.meta.internal.pc.ScalaPresentationCompiler.signatureHelp$$anonfun$1(ScalaPresentationCompiler.scala:388)
```
#### Short summary: 

java.lang.AssertionError: NoDenotation.owner