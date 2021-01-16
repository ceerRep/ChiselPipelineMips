package mips.util

import chisel3._

class Instruction extends Bundle {
  val inst = UInt(32.W)
  val sourseReg = UInt(5.W)
  val targetReg = UInt(5.W)
  val destReg = UInt(5.W)
  val shiftAmount = UInt(5.W)
  val imm16 = UInt(16.W)
  val absulute_jump = UInt(26.W)
}
