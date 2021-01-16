package mips.modules

import chisel3._
import chisel3.util._
import mips.util.Ops

class ALU extends Module {
  val io = IO(new Bundle {
    val op1 = Input(UInt(32.W))
    val op2 = Input(UInt(32.W))
    val op = Input(chiselTypeOf(Ops.add))

    val result = Output(UInt(32.W))
  })

  val shiftAmount = io.op1(4, 0)

  io.result := 0.U(32.W)

  switch(io.op) {
    is (Ops.add) {
      io.result := io.op1 + io.op2
    }
    is (Ops.sub) {
      io.result := io.op1 - io.op2
    }
    is (Ops.shift_r_a) {
      io.result := (io.op2.asSInt() >> shiftAmount).asUInt()
    }
    is (Ops.shift_r_l) {
      io.result := io.op2 >> shiftAmount
    }
    is (Ops.shift_l) {
      io.result := io.op2 << shiftAmount
    }
    is (Ops.and) {
      io.result := io.op1 & io.op2
    }
    is (Ops.or) {
      io.result := io.op1 | io.op2
    }
    is (Ops.xor) {
      io.result := io.op1 ^ io.op2
    }
    is (Ops.nor) {
      io.result := ~(io.op1 | io.op2)
    }
    is (Ops.sless) {
      io.result := (io.op1.asSInt() < io.op2.asSInt()).asUInt()
    }
    is (Ops.uless) {
      io.result := (io.op1 < io.op2).asUInt()
    }
  }
}

object ALU extends App {
  //  (new chisel3.stage.ChiselStage).emitVerilog(new GCD,args.union(Array("-td","v")).distinct)
  (new chisel3.stage.ChiselStage).emitVerilog(new ALU, Array("-td", "v"))
}