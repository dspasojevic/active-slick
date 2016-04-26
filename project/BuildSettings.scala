import sbt._
import sbt.Keys._
import com.typesafe.sbt.SbtScalariform

object BuildSettings {

	val projSettings = Seq(
		scalacOptions := Seq("-unchecked", "-deprecation", "-feature", "-Xlint")
	) ++
    SbtScalariform.defaultScalariformSettings
}