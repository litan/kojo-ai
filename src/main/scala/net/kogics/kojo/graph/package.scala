/*
 * Copyright (C) 2019 Lalit Pant <pant.lalit@gmail.com>
 *
 * The contents of this file are subject to the GNU General Public License
 * Version 3 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.gnu.org/copyleft/gpl.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 */
package net.kogics.kojo
import scala.collection.mutable.ArrayBuffer
import scala.collection.mutable.{Map => MMap}
import scala.collection.mutable.{Set => MSet}

package object graph {
  trait Container[T] {
    def add(t: T): Unit
    def remove(): T
    def hasElements: Boolean
  }

  class Stack[T] extends Container[T] {
    var impl: List[T] = Nil
    def add(t: T) {
      impl = t :: impl
    }
    def remove(): T = {
      val ret = impl.head
      impl = impl.tail
      ret
    }
    def hasElements = !impl.isEmpty
  }

  class Queue[T] extends Container[T] {
    val impl = collection.mutable.Queue.empty[T]
    def add(t: T) {
      impl.enqueue(t)
    }
    def remove(): T = {
      impl.dequeue
    }
    def hasElements = !impl.isEmpty
  }

  case class Node[T](data: T)
  case class EdgeTo[T](node: Node[T], distance: Double = 1)

  type GraphEdges[T] = MMap[Node[T], MSet[EdgeTo[T]]]
  type Nodes[T] = MSet[Node[T]]
  type PathEdges[T] = Seq[EdgeTo[T]]
  type ContainerElem[T] = (Node[T], PathEdges[T])

  trait Graph[T] {
    def nodes: Nodes[T]
    def edges: GraphEdges[T]
  }

  case class ExplicitGraph[T](nodes: Nodes[T], edges: GraphEdges[T]) extends Graph[T]

  object GraphSearch {
    def noOpCallback[T](n: Node[T]) {}
    def dfs[T](graph: Graph[T], start: Node[T], end: Node[T],
               visitCallback: Node[T] => Unit = noOpCallback _): Option[PathEdges[T]] = {
      val container = new Stack[ContainerElem[T]]
      search(graph, start, end, container, visitCallback)
    }

    def bfs[T](graph: Graph[T], start: Node[T], end: Node[T],
               visitCallback: Node[T] => Unit = noOpCallback _): Option[PathEdges[T]] = {
      val container = new Queue[ContainerElem[T]]
      search(graph, start, end, container, visitCallback)
    }

    def search[T](graph: Graph[T], start: Node[T], end: Node[T],
                  container:     Container[ContainerElem[T]],
                  visitCallback: Node[T] => Unit): Option[PathEdges[T]] = {
      if (start == end) {
        Some(ArrayBuffer())
      }
      else {
        container.add((start, ArrayBuffer()))
        val visited = new collection.mutable.HashSet[Node[T]]
        while (container.hasElements) {
          val elem = container.remove
          val elemNode = elem._1
          val elemPath = elem._2
          if (!visited.contains(elemNode)) {
            visitCallback(elemNode)
            visited.add(elemNode)
            if (elemNode == end) {
              return Some(elemPath)
            }
            else {
              graph.edges(elem._1).foreach { edgeTo =>
                container.add((edgeTo.node, elemPath :+ edgeTo))
              }
            }
          }
        }
        None
      }
    }
  }
}
