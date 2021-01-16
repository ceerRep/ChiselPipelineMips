module InstMemoryBlackBox (
    input [31:0] addr0,
    input [31:0] addr1,
    output [31:0] dout0,
    output [31:0] dout1
);
    reg [31:0] mem[2047:0];
    wire [10:0] index0, index1;

    assign index0 = addr0[12:2];
    assign index1 = addr1[12:2];
    initial $readmemh("/tmp/code.txt", mem);

    assign dout0 = mem[index0];
    assign dout1 = mem[index1];
endmodule