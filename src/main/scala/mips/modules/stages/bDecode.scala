package mips.modules.stages

import chisel3._
import chisel3.util._
import mips.modules.PC.JumpType
import mips.modules.{BypassUnit, ControlUnit}
import mips.util.BypassRegData.bypassRegDatas
import mips.util.Control._
import mips.util.{BypassRegData, Control, ControlSignal, GPR, Instruction}

class PipelineDecodeResult extends Bundle {
  val pc = SInt(32.W)
  val pcChanged = Bool()
  val inst = new Instruction
  val controlSignal = new ControlSignal
  val regReadId = new GPR.RegisterReadId
  val regReadData = new GPR.RegisterReadData
  val wRegId = UInt(5.W)
  val wRegDataReady = Bool()
  val wRegData = UInt(32.W)
  val bubbled = Bool()
}

class bDecode extends Module {
  val io = IO(new Bundle {
    val pipelineFetchResult = Input(new PipelineFetchResult)
    val stallOnExecuation = Input(Bool())
    val stallFromExecuation = Input(Bool())
    val regReadData = Input(new GPR.RegisterReadData)

    val regReadId = Output(new GPR.RegisterReadId)
    val pipelineDecodeResult = Output(new PipelineDecodeResult)
    val decodeStall = Output(Bool())
    val dataFromDecode = Output(new BypassRegData)
    val jump = Output(Bool())
    val jumpValue = Output(SInt(32.W))

    // Bypass Query
    val bypass = new Bundle {
      val pcChanged = Output(Bool())
      val regReadId = Output(new GPR.RegisterReadId)
      val origRegData = Output(new GPR.RegisterReadData)
      val bypassData = Input(Vec(2, new Bundle {
        val data = UInt(32.W)
        val stall = Bool()
      }))
    }
  })
  val dataFromDecode = Wire(new BypassRegData)
  io.dataFromDecode := dataFromDecode
  val pipelineDecodeResult = RegInit(0.U.asTypeOf(new PipelineDecodeResult))
  io.pipelineDecodeResult := pipelineDecodeResult

  val cu = Module(new ControlUnit)
  cu.io.inst := io.pipelineFetchResult.inst
  val inst = io.pipelineFetchResult.inst
  val signal = cu.io.controlSignal

  val regReadId1 = Control.getRegisterId(signal.reg1IDFrom, inst)
  val regReadId2 = Control.getRegisterId(signal.reg2IDFrom, inst)

  io.regReadId.id1 := regReadId1
  io.regReadId.id2 := regReadId2

  val wRegId = Control.getRegisterId(signal.wRegIDFrom, inst)

  val hazardStall = Wire(Vec(2, Bool()))

  io.bypass.pcChanged := io.pipelineFetchResult.pcChanged
  io.bypass.regReadId.id1 := regReadId1
  io.bypass.regReadId.id2 := regReadId2
  io.bypass.origRegData := io.regReadData

  hazardStall(0) := io.bypass.bypassData(0).stall
  hazardStall(1) := io.bypass.bypassData(1).stall

  val regReadData1 = io.bypass.bypassData(0).data
  val regReadData2 = io.bypass.bypassData(1).data

  val stallFromDecode = (hazardStall(0) && signal.regData1Stage <= reg_stage_decode) ||
    (hazardStall(1) && signal.regData2Stage <= reg_stage_decode)

  val stallOnDecode = stallFromDecode || io.stallOnExecuation
  io.decodeStall := stallOnDecode

  io.jump := 0.B
  when(!stallOnDecode) {
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

  when(!stallOnDecode) {
    pipelineDecodeResult.pc := io.pipelineFetchResult.pc
    pipelineDecodeResult.pcChanged := io.pipelineFetchResult.pcChanged
    pipelineDecodeResult.inst := io.pipelineFetchResult.inst
    pipelineDecodeResult.controlSignal := signal
    pipelineDecodeResult.regReadId.id1 := regReadId1
    pipelineDecodeResult.regReadId.id2 := regReadId2
    pipelineDecodeResult.regReadData.data1 := regReadData1
    pipelineDecodeResult.regReadData.data2 := regReadData2
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

  when (!io.stallFromExecuation) {
    pipelineDecodeResult.bubbled := stallOnDecode
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