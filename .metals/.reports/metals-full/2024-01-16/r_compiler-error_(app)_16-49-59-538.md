file://<WORKSPACE>/app/src/main/scala/com/rockthejvm/jobsboard/App.scala
### java.lang.AssertionError: NoDenotation.owner

occurred in the presentation compiler.

action parameters:
offset: 167
uri: file://<WORKSPACE>/app/src/main/scala/com/rockthejvm/jobsboard/App.scala
text:
```scala
package com.rockthejvm.jobsboard

import scala.scalajs.js.annotation.*
import org.scalajs.dom.document

@JSExportTopLevel("RockTheJvmApp")
class App extends TyrianApp[@@{
  @JSExport
  def doSomething(containerId: String) =
    document.getElementById(containerId).innerHTML = "THIS ROCKS!"

  //  in JS: document.getElementById(...).innerHTML = "THIS IS MY CREATED HTML"
}

```



#### Error stacktrace:

```
dotty.tools.dotc.core.SymDenotations$NoDenotation$.owner(SymDenotations.scala:2582)
	scala.meta.internal.pc.SignatureHelpProvider$.isValid(SignatureHelpProvider.scala:83)
	scala.meta.internal.pc.SignatureHelpProvider$.notCurrentApply(SignatureHelpProvider.scala:94)
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