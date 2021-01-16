package mips.modules.stages

import chisel3._
import chisel3.util._
import mips.modules.PCControl.JumpType
import mips.modules.ControlUnit
import mips.util.Control._
import mips.util.pipeline._
import mips.util.{BypassRegData, Control, ControlSignal, GPR, Instruction}

class bDecode extends Module {
  val io = IO(new Bundle {
    val pipelineFetchResult = Input(new FetchBundle)
    val suspendOnExecuation = Input(Bool())
    val suspendFromExecuation = Input(Bool())
    val regReadData = Input(GPR.registerReadData)

    val regReadId = Output(GPR.registerReadId)
    val pipelineDecodeResult = Output(new DecodeBundle)
    val decodeSuspend = Output(Bool())
    val dataFromDecode = Output(new BypassRegData)
    val jump = Output(Bool())
    val jumpValue = Output(SInt(32.W))

    // Bypass Query
    val bypass = new Bundle {
      val pcMagic = Output(Bool())
      val regReadId = Output(GPR.registerReadId)
      val origRegData = Output(GPR.registerReadData)
      val bypassData = Input(Vec(2, new Bundle {
        val data = UInt(32.W)
        val suspend = Bool()
      }))
    }
  })
  val dataFromDecode = Wire(new BypassRegData)
  io.dataFromDecode := dataFromDecode
  val pipelineDecodeResult = RegInit(0.U.asTypeOf(new DecodeBundle))
  io.pipelineDecodeResult := pipelineDecodeResult

  val cu = Module(new ControlUnit)
  cu.io.inst := io.pipelineFetchResult.inst
  val inst = io.pipelineFetchResult.inst
  val signal = cu.io.controlSignal

  val regReadId1 = Control.getRegisterId(signal.reg1IDFrom, inst)
  val regReadId2 = Control.getRegisterId(signal.reg2IDFrom, inst)

  io.regReadId(0) := regReadId1
  io.regReadId(1) := regReadId2

  val wRegId = Control.getRegisterId(signal.wRegIDFrom, inst)

  val regDataNotReady = Wire(Vec(2, Bool()))
  regDataNotReady(0) := io.bypass.bypassData(0).suspend
  regDataNotReady(1) := io.bypass.bypassData(1).suspend

  io.bypass.pcMagic := io.pipelineFetchResult.pcMagic
  io.bypass.regReadId(0) := regReadId1
  io.bypass.regReadId(1) := regReadId2
  io.bypass.origRegData := io.regReadData

  val regReadData1 = io.bypass.bypassData(0).data
  val regReadData2 = io.bypass.bypassData(1).data

  val suspendFromDecode = (regDataNotReady(0) && signal.regData1Stage <= reg_stage_decode) ||
    (regDataNotReady(1) && signal.regData2Stage <= reg_stage_decode)

  val suspendOnDecode = suspendFromDecode || io.suspendOnExecuation
  io.decodeSuspend := suspendOnDecode

  io.jump := 0.B
  when(!suspendOnDecode) {
    switch(signal.jumpCond) {
      is(jump_always) {
        io.jump := 1.B
      }
      is(jump_never) {
        io.jump := 0.B
      }
      is(jump_eq) {
        io.jump := regReadData1 === regReadData2
      }
      is(jump_neq) {
        io.jump := regReadData1 =/= regReadData2
      }
      is(jump_ltz) {
        io.jump := regReadData1.asSInt() < 0.S(32.W)
      }
      is(jump_lez) {
        io.jump := regReadData1.asSInt() <= 0.S(32.W)
      }
      is(jump_gtz) {
        io.jump := regReadData1.asSInt() > 0.S(32.W)
      }
      is(jump_gez) {
        io.jump := regReadData1.asSInt() >= 0.S(32.W)
      }
    }
  }

  val jumpInput = getJumpValue(signal.jumpValFrom, regReadData1, inst)
  val jumpValue = WireDefault(0.S(32.W))
  io.jumpValue := jumpValue

  switch(signal.jumpType) {
    is(JumpType.near) {
      jumpValue := (io.pipelineFetchResult.pc & 0xF0000000.S(32.W)) | Cat(jumpInput(25, 0), 0.U(2.W)).asSInt()
    }
    is(JumpType.far) {
      jumpValue := jumpInput.asSInt()
    }
    is(JumpType.relative) {
      jumpValue := io.pipelineFetchResult.pc + 4.S(32.W) + Cat(jumpInput(29, 0), 0.U(2.W)).asSInt()
    }
  }

  when(!suspendOnDecode) {
    pipelineDecodeResult.pc := io.pipelineFetchResult.pc
    pipelineDecodeResult.pcMagic := io.pipelineFetchResult.pcMagic
    pipelineDecodeResult.inst := io.pipelineFetchResult.inst
    pipelineDecodeResult.controlSignal := signal
    pipelineDecodeResult.regReadId(0) := regReadId1
    pipelineDecodeResult.regReadId(1) := regReadId2
    pipelineDecodeResult.regReadData(0) := regReadData1
    pipelineDecodeResult.regReadData(1) := regReadData2
    pipelineDecodeResult.wRegId := wRegId

    when(signal.wRegEnabled) {
      when(signal.wRegDataFrom === reg_write_from_pc_8) {
        pipelineDecodeResult.wRegDataReady := 1.B
        pipelineDecodeResult.wRegData := io.pipelineFetchResult.pc.asUInt() + 8.U(32.W)
      }.elsewhen(signal.wRegDataFrom === reg_write_from_imme_lsh) {
        pipelineDecodeResult.wRegDataReady := 1.B
        pipelineDecodeResult.wRegData := Cat(io.pipelineFetchResult.inst.imm16, 0.U(16.W))
      }.otherwise {
        pipelineDecodeResult.wRegDataReady := 0.B
        pipelineDecodeResult.wRegData := 0x55AA55AA.U(32.W)
      }
    }.otherwise {
      pipelineDecodeResult.wRegDataReady := 1.B
      pipelineDecodeResult.wRegData := 0x00C00C00.U(32.W)
    }


  }

  when (!io.suspendFromExecuation) {
    pipelineDecodeResult.bubbled := suspendOnDecode
  }

  when (pipelineDecodeResult.bubbled) {
    dataFromDecode.regId := 0.U(5.W)
    dataFromDecode.data := 0.U(32.W)
    dataFromDecode.dataReady := 1.B
  }.otherwise {
    dataFromDecode.regId := pipelineDecodeResult.wRegId
    dataFromDecode.data := pipelineDecodeResult.wRegData
    dataFromDecode.dataReady := pipelineDecodeResult.wRegDataReady
  }

}

object bDecode extends App {
  //  (new chisel3.stage.ChiselStage).emitVerilog(new GCD,args.union(Array("-td","v")).distinct)
  (new chisel3.stage.ChiselStage).emitVerilog(new bDecode, Array("-td", "v"))
}