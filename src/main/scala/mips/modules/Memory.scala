package mips.modules

import chisel3._
import mips.{DataMemoryBlackBox, InstMemoryBlackBox}

class Memory extends Module {
  val io = IO(new Bundle {
    val readonly = new Bundle {
      val addr = Input(UInt(32.W))
      val readSize = Input(UInt(2.W))
      val readSign = Input(Bool())

      val readData = Output(UInt(32.W))
    }

    val readwrite = new Bundle {
      val pc = Input(SInt(32.W))
      val addr = Input(UInt(32.W))
      val readSize = Input(UInt(2.W))
      val readSign = Input(Bool())
      val writeSize = Input(UInt(2.W))
      val writeData = Input(UInt(32.W))

      val readData = Output(UInt(32.W))
    }
  })

  io.readonly.readSize := DontCare
  io.readonly.readSign := DontCare
  io.readonly.readData := 0.U

  io.readwrite.readData := 0.U

  // 0x0000 - 0x2000
  val dm = Module(new DataMemoryBlackBox)

  dm.io.clock := clock
  dm.io.pc := 0.S
  dm.io.write_size := 0.U
  dm.io.read_sign_extend := 0.U
  dm.io.read_size := 0.U
  dm.io.addr := 0.U

  // 0x3000 - 0x5000
  val im = Module(new InstMemoryBlackBox)
  im.io.addr0 := 0.U
  im.io.addr1 := 0.U

  val rAddr = io.readonly.addr
  when (0.U <= rAddr && rAddr < 0x2000.U) {
    // This shouldn't happen
    assert(!(0.U <= rAddr && rAddr < 0x2000.U))
  }.elsewhen(0x3000.U <= rAddr && rAddr < 0x5000.U) {
    im.io.addr1 := rAddr - 0x3000.U
    io.readonly.readData := im.io.dout1
  }

  val rwAddr = io.readwrite.addr
  when (0.U <= rwAddr & rwAddr < 0x2000.U) {
    dm.io.write_size := io.readwrite.writeSize
    dm.io.read_size := io.readwrite.readSize
    dm.io.read_sign_extend := io.readwrite.readSign
    dm.io.addr := io.readwrite.addr
    dm.io.din := io.readwrite.writeData
    dm.io.pc := io.readwrite.pc

    io.readwrite.readData := dm.io.dout
  }.elsewhen(0x3000.U <= rwAddr && rwAddr < 0x5000.U) {
    im.io.addr0 := rwAddr - 0x3000.U
    io.readwrite.readData := im.io.dout0
  }
}
