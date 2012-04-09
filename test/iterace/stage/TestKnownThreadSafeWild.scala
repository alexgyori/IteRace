package iterace.stage

import org.junit.Test
import iterace.util.log
import iterace.util.debug

class TestKnownThreadSafeWild extends RaceAbstractTest("Lparticles/ParticleUsingLibrary") {
  
  log.activate
  debug.activate
  debug.activateDetailedContexts

  analysisScope.addBinaryDependency("particles");

  @Test def noRaceWhenPrintln = expectNoRaces
  @Test def noRaceOnPattern = expectNoRaces
  @Test def noRaceOnSafeMatcher = expectNoRaces
  @Test def raceOnUnsafeMatcher = expectSomeRaces
  
//  @Test def racePastKnownThreadSafe = expect("""
//Loop: particles.ParticleWithKnownThreadSafe.racePastKnownThreadSafe(ParticleWithKnownThreadSafe.java:35)
//
//particles.Particle: particles.ParticleWithKnownThreadSafe.racePastKnownThreadSafe(ParticleWithKnownThreadSafe.java:33)
// .x
//   (a)  particles.Particle.moveTo(Particle.java:16)
//   (b)  particles.Particle.moveTo(Particle.java:16)
// .y
//   (a)  particles.Particle.moveTo(Particle.java:17)
//   (b)  particles.Particle.moveTo(Particle.java:17)
//""")

}