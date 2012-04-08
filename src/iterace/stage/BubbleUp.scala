package iterace.stage
import com.ibm.wala.util.graph.traverse.DFS
import com.ibm.wala.util.graph.impl.GraphInverter
import com.ibm.wala.util.collections.Filter
import iterace.util.S
import iterace.util.WALAConversions._
import iterace.util.crossProduct
import scala.collection.JavaConverters._
import scala.collection._
import iterace.pointeranalysis.RacePointerAnalysis
import iterace.datastructure.ProgramRaceSet
import iterace.datastructure.ShallowRace
import iterace.datastructure.LowLevelRaceSet
import iterace.datastructure.ShallowRaceSet
import iterace.datastructure.FieldRaceSet

class BubbleUp(pa: RacePointerAnalysis) extends Stage {
  import pa._

  def groupByObject(accesses: Set[S[I]]): Map[O, Set[S[I]]] = {
    accesses.flatMap[(O, S[I]), Set[(O, S[I])]](s =>
      if (s.isStatic)
        Set((unknownO, s))
      else
        s.refP.get.pt map { (_, s) }) groupBy { _._1 } map { case (o, set) => (o, set map { _._2 }) }
  }

  def containsShallowAccess(accesses: Set[S[I]]) =
    accesses.exists(s => {!s.i.isInstanceOf[AccessI] && !s.i.isInstanceOf[ArrayReferenceI]})

  // the if is there because we are not interested in the objects that are written but not read
  // so, if an object is pointed by the alphaAcccesses is not pointed by a beta access, we 
  // can simply ignore it and it is safe to do so
  def zipByKey[K, VA, VB](a: Map[K, VA], b: Map[K, VB]): Map[K, (VA, VB)] = a collect { case (k, v) if b.contains(k) => (k, (v, b(k))) }

  def apply(races: ProgramRaceSet): ProgramRaceSet = {
    var i = 0

    ProgramRaceSet.fromRaceSets(races.getLowLevelRaceSets flatMap (raceset => {
      val aAppLevelAccesses = raceset.alphaAccesses flatMap { bubbleUp(_) }
      val bAppLevelAccesses = raceset.betaAccesses flatMap { bubbleUp(_) }

      val aGroupedByObject = groupByObject(aAppLevelAccesses)
      val bGroupedByObject = groupByObject(bAppLevelAccesses)

      zipByKey(aGroupedByObject, bGroupedByObject) flatMap {
        case (o, (aAccesses, bAccesses)) => {
          val innerSet: Set[LowLevelRaceSet] =
            if (containsShallowAccess(aAccesses) || containsShallowAccess(bAccesses))
              Set(new ShallowRaceSet(raceset.l, o, aAccesses, bAccesses))
            else {
              val aAccessesByF = aAccesses groupBy { _.i.f }
              val bAccessesByF = bAccesses groupBy { _.i.f }
              zipByKey(aAccessesByF, bAccessesByF) map {
                case (f, (aAccs, bAccs)) =>
                  new FieldRaceSet(raceset.l, o, f.get, aAccs, bAccessesByF(f))
              } toSet
            }
          innerSet
        }
      } toSet
    }))

//    ProgramRaceSet(
//      races map (r => {
//        if (inPrimordialScope(r.a.n) || inPrimordialScope(r.b.n)) {
//          val alphaAppLevelAccesses = bubbleUp(r.a)
//          val betaAppLevelAccesses = bubbleUp(r.b)
//
//          val pairs = crossProduct(alphaAppLevelAccesses, betaAppLevelAccesses)
//
//          //        i += 1; println(i + "  " + pairs.size + "   :    "+r.a+"   X   "+r.b)
//
//          pairs collect {
//            case (s1, s2) => {
//              val objects =
//                if (!s1.isStatic && !s2.isStatic)
//                  s1.refP.get.pt & s2.refP.get.pt
//                else
//                  Seq(unknownO)
//              objects collect { case o => new ShallowRace(r.l, o, s1, s2) }
//            }
//          } flatten
//        } else
//          Set(r)
//      }) flatten)
  }

  private val bubbledUp: mutable.Map[S[I], Set[S[I]]] = mutable.Map()

  def bubbleUp(s: S[I]): Set[S[I]] = {
    if (inApplicationScope(s.n))
      return Set(s)

    bubbledUp.getOrElseUpdate(s, {
      val nodesInPrimordial = DFS.getReachableNodes(GraphInverter.invert(callGraph), Seq(s.n).asJava,
        new Filter[N]() {
          def accepts(n: N) = inPrimordialScope(n)
        }) asScala

      val invokeSs = (for (
        n <- nodesInPrimordial;
        predN <- callGraph.getPredNodes(n).asScala if inApplicationScope(predN)
      ) yield {
        val callSites = callGraph.getPossibleSites(predN, n).asScala
        val invokeIs = callSites map { predN.getIR().getCalls(_) } flatten
        val invokeSs = invokeIs map { S(predN, _) }
        invokeSs
      })
      return invokeSs.toSet.flatten
    })
  }
}

object BubbleUp extends StageConstructor {
  def apply(pa: RacePointerAnalysis) = {
    new BubbleUp(pa)
  }
}