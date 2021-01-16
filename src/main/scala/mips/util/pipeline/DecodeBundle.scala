package mips.util.pipeline

import chisel3._
import mips.util.{ControlSignal, GPR, Instruction}

class DecodeBundle extends Bundle {
  val pc = SInt(32.W)
  val pcMagic = Bool()
  val inst = new Instruction
  val controlSignal = new ControlSignal
  val regReadId = GPR.registerReadId
  val regReadData = GPR.registerReadData
  val wRegId = UInt(5.W)
  val wRegDataReady = Bool()
  val wRegData = UInt(32.W)
  val bubbled = Bool()
}
