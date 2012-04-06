package iterace
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class TestPossibleRacesNow extends RaceAbstractTest("Lparticles/Particle") {
  
	override val stages: Seq[Stage] = Seq()
  analysisScope.addBinaryDependency("particles");
  
}