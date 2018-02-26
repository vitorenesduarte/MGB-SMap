package org.telecomsudparis

package object smap {
  implicit def funcToRunnable( func : () => Unit ) = new Runnable(){ def run() = func()}
}
