jar:file://<HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-library/2.12.18/scala-library-2.12.18-sources.jar!/scala/collection/Seq.scala
### jar%3Afile%3A%2F%2F%2Fhome%2Fjkubalec%2F.cache%2Fcoursier%2Fv1%2Fhttps%2Frepo1.maven.org%2Fmaven2%2Forg%2Fscala-lang%2Fscala-library%2F2.12.18%2Fscala-library-2.12.18-sources.jar%21%2Fscala%2Fcollection%2FSeq.scala:20: error: ; expected but package found
package collection
^

occurred in the presentation compiler.

action parameters:
uri: jar:file://<HOME>/.cache/coursier/v1/https/repo1.maven.org/maven2/org/scala-lang/scala-library/2.12.18/scala-library-2.12.18-sources.jar!/scala/collection/Seq.scala
text:
```scala
import _root_.scala.xml.{TopScope=>$scope}
import _root_.sbt._
import _root_.sbt.Keys._
import _root_.sbt.nio.Keys._
import _root_.sbt.ScriptedPlugin.autoImport._, _root_.sbt.plugins.JUnitXmlReportPlugin.autoImport._, _root_.sbt.plugins.MiniDependencyTreePlugin.autoImport._, _root_.bloop.integrations.sbt.BloopPlugin.autoImport._, _root_.com.typesafe.sbt.SbtNativePackager.autoImport._, _root_.com.typesafe.sbt.packager.archetypes.JavaAppPackaging.autoImport._, _root_.com.typesafe.sbt.packager.archetypes.JavaServerAppPackaging.autoImport._, _root_.com.typesafe.sbt.packager.archetypes.jar.ClasspathJarPlugin.autoImport._, _root_.com.typesafe.sbt.packager.archetypes.jar.LauncherJarPlugin.autoImport._, _root_.com.typesafe.sbt.packager.archetypes.jlink.JlinkPlugin.autoImport._, _root_.com.typesafe.sbt.packager.archetypes.scripts.BashStartScriptPlugin.autoImport._, _root_.com.typesafe.sbt.packager.archetypes.scripts.BatStartScriptPlugin.autoImport._, _root_.com.typesafe.sbt.packager.archetypes.systemloader.SystemdPlugin.autoImport._, _root_.com.typesafe.sbt.packager.archetypes.systemloader.SystemloaderPlugin.autoImport._, _root_.com.typesafe.sbt.packager.debian.DebianPlugin.autoImport._, _root_.com.typesafe.sbt.packager.docker.DockerPlugin.autoImport._, _root_.com.typesafe.sbt.packager.graalvmnativeimage.GraalVMNativeImagePlugin.autoImport._, _root_.com.typesafe.sbt.packager.jdkpackager.JDKPackagerPlugin.autoImport._, _root_.com.typesafe.sbt.packager.linux.LinuxPlugin.autoImport._, _root_.com.typesafe.sbt.packager.rpm.RpmPlugin.autoImport._, _root_.com.typesafe.sbt.packager.universal.UniversalPlugin.autoImport._, _root_.com.typesafe.sbt.packager.windows.WindowsPlugin.autoImport._, _root_.org.scalajs.sbtplugin.ScalaJSPlugin.autoImport._, _root_.scalajsbundler.sbtplugin.ScalaJSBundlerPlugin.autoImport._, _root_.scalajscrossproject.ScalaJSCrossPlugin.autoImport._, _root_.org.portablescala.sbtplatformdeps.PlatformDepsPlugin.autoImport._, _root_.sbtcrossproject.CrossPlugin.autoImport._
import _root_.sbt.plugins.IvyPlugin, _root_.sbt.plugins.JvmPlugin, _root_.sbt.plugins.CorePlugin, _root_.sbt.ScriptedPlugin, _root_.sbt.plugins.SbtPlugin, _root_.sbt.plugins.SemanticdbPlugin, _root_.sbt.plugins.JUnitXmlReportPlugin, _root_.sbt.plugins.Giter8TemplatePlugin, _root_.sbt.plugins.MiniDependencyTreePlugin, _root_.bloop.integrations.sbt.BloopPlugin, _root_.com.typesafe.sbt.SbtNativePackager, _root_.com.typesafe.sbt.packager.archetypes.JavaAppPackaging, _root_.com.typesafe.sbt.packager.archetypes.JavaServerAppPackaging, _root_.com.typesafe.sbt.packager.archetypes.jar.ClasspathJarPlugin, _root_.com.typesafe.sbt.packager.archetypes.jar.LauncherJarPlugin, _root_.com.typesafe.sbt.packager.archetypes.jlink.JlinkPlugin, _root_.com.typesafe.sbt.packager.archetypes.scripts.AshScriptPlugin, _root_.com.typesafe.sbt.packager.archetypes.scripts.BashStartScriptPlugin, _root_.com.typesafe.sbt.packager.archetypes.scripts.BatStartScriptPlugin, _root_.com.typesafe.sbt.packager.archetypes.systemloader.SystemVPlugin, _root_.com.typesafe.sbt.packager.archetypes.systemloader.SystemdPlugin, _root_.com.typesafe.sbt.packager.archetypes.systemloader.SystemloaderPlugin, _root_.com.typesafe.sbt.packager.archetypes.systemloader.UpstartPlugin, _root_.com.typesafe.sbt.packager.debian.DebianDeployPlugin, _root_.com.typesafe.sbt.packager.debian.DebianPlugin, _root_.com.typesafe.sbt.packager.debian.JDebPackaging, _root_.com.typesafe.sbt.packager.docker.DockerPlugin, _root_.com.typesafe.sbt.packager.docker.DockerSpotifyClientPlugin, _root_.com.typesafe.sbt.packager.graalvmnativeimage.GraalVMNativeImagePlugin, _root_.com.typesafe.sbt.packager.jdkpackager.JDKPackagerDeployPlugin, _root_.com.typesafe.sbt.packager.jdkpackager.JDKPackagerPlugin, _root_.com.typesafe.sbt.packager.linux.LinuxPlugin, _root_.com.typesafe.sbt.packager.rpm.RpmDeployPlugin, _root_.com.typesafe.sbt.packager.rpm.RpmPlugin, _root_.com.typesafe.sbt.packager.universal.UniversalDeployPlugin, _root_.com.typesafe.sbt.packager.universal.UniversalPlugin, _root_.com.typesafe.sbt.packager.windows.WindowsDeployPlugin, _root_.com.typesafe.sbt.packager.windows.WindowsPlugin, _root_.org.scalajs.sbtplugin.ScalaJSJUnitPlugin, _root_.org.scalajs.sbtplugin.ScalaJSPlugin, _root_.scalajsbundler.sbtplugin.ScalaJSBundlerPlugin, _root_.scalajscrossproject.ScalaJSCrossPlugin, _root_.org.portablescala.sbtplatformdeps.PlatformDepsPlugin, _root_.sbtcrossproject.CrossPlugin
/*
 * Scala (https://www.scala-lang.org)
 *
 * Copyright EPFL and Lightbend, Inc.
 *
 * Licensed under Apache License 2.0
 * (http://www.apache.org/licenses/LICENSE-2.0).
 *
 * See the NOTICE file distributed with this work for
 * additional information regarding copyright ownership.
 */

package scala
package collection

import generic._
import mutable.Builder

/** A base trait for sequences.
 *  $seqInfo
 */
trait Seq[+A] extends PartialFunction[Int, A]
                      with Iterable[A]
                      with GenSeq[A]
                      with GenericTraversableTemplate[A, Seq]
                      with SeqLike[A, Seq[A]] {
  override def companion: GenericCompanion[Seq] = Seq

  override def seq: Seq[A] = this
}

/** $factoryInfo
 *  The current default implementation of a $Coll is a `List`.
 *  @define coll sequence
 *  @define Coll `Seq`
 */
object Seq extends SeqFactory[Seq] {
  /** $genericCanBuildFromInfo */
  implicit def canBuildFrom[A]: CanBuildFrom[Coll, A, Seq[A]] = ReusableCBF.asInstanceOf[GenericCanBuildFrom[A]]

  def newBuilder[A]: Builder[A, Seq[A]] = immutable.Seq.newBuilder[A]
}

/** Explicit instantiation of the `Seq` trait to reduce class file size in subclasses. */
abstract class AbstractSeq[+A] extends AbstractIterable[A] with Seq[A]

```



#### Error stacktrace:

```
scala.meta.internal.parsers.Reporter.syntaxError(Reporter.scala:16)
	scala.meta.internal.parsers.Reporter.syntaxError$(Reporter.scala:16)
	scala.meta.internal.parsers.Reporter$$anon$1.syntaxError(Reporter.scala:22)
	scala.meta.internal.parsers.Reporter.syntaxError(Reporter.scala:17)
	scala.meta.internal.parsers.Reporter.syntaxError$(Reporter.scala:17)
	scala.meta.internal.parsers.Reporter$$anon$1.syntaxError(Reporter.scala:22)
	scala.meta.internal.parsers.ScalametaParser.syntaxErrorExpected(ScalametaParser.scala:421)
	scala.meta.internal.parsers.ScalametaParser.expect(ScalametaParser.scala:423)
	scala.meta.internal.parsers.ScalametaParser.accept(ScalametaParser.scala:427)
	scala.meta.internal.parsers.ScalametaParser.acceptStatSep(ScalametaParser.scala:447)
	scala.meta.internal.parsers.ScalametaParser.acceptStatSepOpt(ScalametaParser.scala:451)
	scala.meta.internal.parsers.ScalametaParser.statSeqBuf(ScalametaParser.scala:4462)
	scala.meta.internal.parsers.ScalametaParser.$anonfun$batchSource$13(ScalametaParser.scala:4696)
	scala.Option.getOrElse(Option.scala:189)
	scala.meta.internal.parsers.ScalametaParser.$anonfun$batchSource$1(ScalametaParser.scala:4696)
	scala.meta.internal.parsers.ScalametaParser.atPos(ScalametaParser.scala:319)
	scala.meta.internal.parsers.ScalametaParser.autoPos(ScalametaParser.scala:365)
	scala.meta.internal.parsers.ScalametaParser.batchSource(ScalametaParser.scala:4652)
	scala.meta.internal.parsers.ScalametaParser.$anonfun$source$1(ScalametaParser.scala:4645)
	scala.meta.internal.parsers.ScalametaParser.atPos(ScalametaParser.scala:319)
	scala.meta.internal.parsers.ScalametaParser.autoPos(ScalametaParser.scala:365)
	scala.meta.internal.parsers.ScalametaParser.source(ScalametaParser.scala:4645)
	scala.meta.internal.parsers.ScalametaParser.entrypointSource(ScalametaParser.scala:4650)
	scala.meta.internal.parsers.ScalametaParser.parseSourceImpl(ScalametaParser.scala:135)
	scala.meta.internal.parsers.ScalametaParser.$anonfun$parseSource$1(ScalametaParser.scala:132)
	scala.meta.internal.parsers.ScalametaParser.parseRuleAfterBOF(ScalametaParser.scala:59)
	scala.meta.internal.parsers.ScalametaParser.parseRule(ScalametaParser.scala:54)
	scala.meta.internal.parsers.ScalametaParser.parseSource(ScalametaParser.scala:132)
	scala.meta.parsers.Parse$.$anonfun$parseSource$1(Parse.scala:29)
	scala.meta.parsers.Parse$$anon$1.apply(Parse.scala:36)
	scala.meta.parsers.Api$XtensionParseDialectInput.parse(Api.scala:25)
	scala.meta.internal.semanticdb.scalac.ParseOps$XtensionCompilationUnitSource.toSource(ParseOps.scala:17)
	scala.meta.internal.semanticdb.scalac.TextDocumentOps$XtensionCompilationUnitDocument.toTextDocument(TextDocumentOps.scala:206)
	scala.meta.internal.pc.SemanticdbTextDocumentProvider.textDocument(SemanticdbTextDocumentProvider.scala:54)
	scala.meta.internal.pc.ScalaPresentationCompiler.$anonfun$semanticdbTextDocument$1(ScalaPresentationCompiler.scala:374)
```
#### Short summary: 

jar%3Afile%3A%2F%2F%2Fhome%2Fjkubalec%2F.cache%2Fcoursier%2Fv1%2Fhttps%2Frepo1.maven.org%2Fmaven2%2Forg%2Fscala-lang%2Fscala-library%2F2.12.18%2Fscala-library-2.12.18-sources.jar%21%2Fscala%2Fcollection%2FSeq.scala:20: error: ; expected but package found
package collection
^