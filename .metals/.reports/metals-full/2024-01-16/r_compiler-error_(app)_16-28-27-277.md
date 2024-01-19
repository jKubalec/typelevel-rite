file://<WORKSPACE>/app/src/main/scala/com/rockthejvm/jobsboard/App.scala
### dotty.tools.dotc.ast.Trees$UnAssignedTypeException: type of Apply(Select(New(Ident(JSExportTopLevel)),<init>),List(Literal(Constant(RockTheJvmApp)))) is not assigned

occurred in the presentation compiler.

action parameters:
offset: 84
uri: file://<WORKSPACE>/app/src/main/scala/com/rockthejvm/jobsboard/App.scala
text:
```scala
package com.rockthejvm.jobsboard

import scala.scalajs.js.annotation.*

@JSExportTop@@Level("RockTheJvmApp")
class App {
  
}

```



#### Error stacktrace:

```
dotty.tools.dotc.ast.Trees$Tree.tpe(Trees.scala:72)
	scala.meta.internal.mtags.MtagsEnrichments$.tryTail$1(MtagsEnrichments.scala:330)
	scala.meta.internal.mtags.MtagsEnrichments$.expandRangeToEnclosingApply(MtagsEnrichments.scala:347)
	scala.meta.internal.pc.HoverProvider$.hover(HoverProvider.scala:48)
	scala.meta.internal.pc.ScalaPresentationCompiler.hover$$anonfun$1(ScalaPresentationCompiler.scala:342)
```
#### Short summary: 

dotty.tools.dotc.ast.Trees$UnAssignedTypeException: type of Apply(Select(New(Ident(JSExportTopLevel)),<init>),List(Literal(Constant(RockTheJvmApp)))) is not assigned