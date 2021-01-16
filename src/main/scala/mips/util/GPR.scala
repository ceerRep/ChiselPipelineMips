package mips.util

import chisel3._

object GPR {
  val ZERO = 0.U(5.W)
  val AT   = 1.U(5.W)
  val V0   = 2.U(5.W)
  val V1   = 3.U(5.W)
  val A0   = 4.U(5.W)
  val A1   = 5.U(5.W)
  val A2   = 6.U(5.W)
  val A3   = 7.U(5.W)
  val T0   = 8.U(5.W)
  val T1   = 9.U(5.W)
  val T2   = 10.U(5.W)
  val T3   = 11.U(5.W)
  val T4   = 12.U(5.W)
  val T5   = 13.U(5.W)
  val T6   = 14.U(5.W)
  val T7   = 15.U(5.W)
  val S0   = 16.U(5.W)
  val S1   = 17.U(5.W)
  val S2   = 18.U(5.W)
  val S3   = 19.U(5.W)
  val S4   = 20.U(5.W)
  val S5   = 21.U(5.W)
  val S6   = 22.U(5.W)
  val S7   = 23.U(5.W)
  val T8   = 24.U(5.W)
  val T9   = 25.U(5.W)
  val K0   = 26.U(5.W)
  val K1   = 27.U(5.W)
  val GP   = 28.U(5.W)
  val SP   = 29.U(5.W)
  val FP   = 30.U(5.W)
  val RA   = 31.U(5.W)

  class RegisterReadId extends Bundle {
    val id1 = UInt(5.W)
    val id2 = UInt(5.W)
  }

  class RegisterReadData extends Bundle {
    val data1 = UInt(32.W)
    val data2 = UInt(32.W)
  }
}
