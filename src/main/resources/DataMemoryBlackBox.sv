module DataMemoryBlackBox (
    input clock,
    input [31:0] addr,
    input [31:0] din,
    input [1:0] write_size,
    input [1:0] read_size,
    input read_sign_extend,
    input [31:0] pc,
    output reg [31:0] dout
);
    reg [31:0] mem[2047:0];

    wire [10:0] offset;
    assign offset = addr[12:2];

    wire [31:0] mem_data;
    assign mem_data = mem[offset];

    reg [31:0] data_to_write;

    always @* begin
        dout = 32'b0;
        case (read_size)
            0:
                dout = 32'b0;
            1: begin
                case (addr[1:0])
                    2'd0: dout = {24'b0, mem_data[7:0]};
                    2'd1: dout = {24'b0, mem_data[15:8]};
                    2'd2: dout = {24'b0, mem_data[23:16]};
                    2'd3: dout = {24'b0, mem_data[31:24]};
                endcase

                if (read_sign_extend)
                    dout = {{24 {dout[7]}}, dout[7:0]};
            end
            2: begin
                case (addr[1])
                    1'd0: dout = {16'b0, mem_data[15:0]};
                    1'd1: dout = {16'b0, mem_data[31:16]};
                endcase

                if (read_sign_extend)
                    dout = {{16 {dout[15]}}, dout[15:0]};
            end
            3: dout = mem_data;
        endcase
    end

    always @* begin
        data_to_write = mem_data;

        case (write_size)
            1: begin
                case (addr[1:0])
                    0: data_to_write = {data_to_write[31:8], din[7:0]};
                    1: data_to_write = {data_to_write[31:16], din[7:0], data_to_write[7:0]};
                    2: data_to_write = {data_to_write[31:24], din[7:0], data_to_write[15:0]};
                    3: data_to_write = {din[7:0], data_to_write[23:0]};
                endcase
            end

            2: begin
                case (addr[1])
                    0: data_to_write = {data_to_write[31:16], din[15:0]};
                    1: data_to_write = {din[15:0], data_to_write[15:0]};
                endcase
            end
            3: data_to_write = din;
        endcase
    end

    always @(posedge clock) begin
        if (write_size != 0) begin
            mem[offset] = data_to_write;
            $display("@%h: *%h <= %h", pc, {29'(offset), 2'b0}, data_to_write);
        end
    end

    initial begin
        $readmemh("/tmp/data.txt", mem);
    end
endmodule