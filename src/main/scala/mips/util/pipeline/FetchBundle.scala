package mips.util.pipeline

import chisel3._
import mips.util.Instruction

class FetchBundle extends Bundle {
  val pc = SInt(32.W)
  val pcMagic = Bool()
  val inst = new Instruction
}
