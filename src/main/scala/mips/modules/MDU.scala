package mips.modules

import chisel3._
import chisel3.util._
import mips.util.Ops

class MDU extends Module {
  val io = IO(new Bundle {
    val op1 = Input(UInt(32.W))
    val op2 = Input(UInt(32.W))
    val op = Input(chiselTypeOf(Ops.add))
    val start = Input(Bool())

    val busy = Output(Bool())
    val data = Output(UInt(32.W))
  })

  val hi = RegInit(0.U(32.W))
  val lo = RegInit(0.U(32.W))
  val busyCycle = RegInit(0.U(32.W))

  val op1 = RegInit(0.U(32.W))
  val op2 = RegInit(0.U(32.W))
  val op = RegInit(0.U(32.W))

  when (busyCycle =/= 0.U) {
    when(op === Ops.m_unsign_mul) {
      val res = op1 * op2
      hi := res(63, 32)
      lo := res(31, 0)
    }.elsewhen(op === Ops.m_unsign_div) {
      when(op2 =/= 0.U) {
        hi := (op1 % op2) (31, 0)
        lo := (op1 / op2) (31, 0)
      }
    }.elsewhen(op === Ops.m_sign_mul) {
      val res = (op1.asSInt() * op2.asSInt()).asUInt()
      hi := res(63, 32)
      lo := res(31, 0)
    }.elsewhen(op === Ops.m_sign_div) {
      when(op2 =/= 0.U) {
        hi := (op1.asSInt() % op2.asSInt()).asUInt()(31, 0)
        lo := (op1.asSInt() / op2.asSInt()).asUInt()(31, 0)
      }
    }
  }

  when(busyCycle =/= 0.U(32.W)) {
    busyCycle := busyCycle - 1.U
  }

  when(busyCycle === 0.U(32.W)) {
    when(io.op === Ops.m_sign_div || io.op === Ops.m_sign_mul ||
      io.op === Ops.m_unsign_div || io.op === Ops.m_unsign_mul) {

      when (io.start) {
        busyCycle := 10.U(32.W)
        op := io.op
        op1 := io.op1
        op2 := io.op2
      }
    }.elsewhen(io.op === Ops.m_write_hi) {
      hi := io.op1
    }.elsewhen(io.op === Ops.m_write_lo) {
      lo := io.op1
    }
  }

  io.busy := busyCycle =/= 0.U

  io.data := Mux(busyCycle =/= 0.U, 0x00CC00CC.U(32.W), MuxCase(0.U(32.W), Array(
    (io.op === Ops.m_read_hi) -> hi,
    (io.op === Ops.m_read_lo) -> lo
  )))

}

object MDU extends App {
  //  (new chisel3.stage.ChiselStage).emitVerilog(new GCD,args.union(Array("-td","v")).distinct)
  (new chisel3.stage.ChiselStage).emitVerilog(new MDU, Array("-td", "v"))
}