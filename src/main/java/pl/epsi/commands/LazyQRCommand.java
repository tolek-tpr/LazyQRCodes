package pl.epsi.commands;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.qrcode.encoder.Encoder;
import com.google.zxing.qrcode.encoder.QRCode;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.command.argument.ArgumentTypes;
import net.minecraft.command.argument.MessageArgumentType;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import pl.epsi.LazyQRCodes;

import static net.minecraft.server.command.CommandManager.*;

public class LazyQRCommand implements ModInitializer {

    public int findSmallestQRVersion(String data) throws WriterException {
        QRCode qrCode = Encoder.encode(data, ErrorCorrectionLevel.L); // Low error correction for smallest size
        return qrCode.getVersion().getVersionNumber();
    }

    public boolean[][] generateQRMatrix(String text) throws WriterException {
        int size = 17 + (findSmallestQRVersion(text) * 4) + 8;
        QRCodeWriter qrWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrWriter.encode(text, BarcodeFormat.QR_CODE, size, size);

        boolean[][] qrArray = new boolean[size][size];

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                qrArray[y][x] = bitMatrix.get(x, y);
            }
        }
        return qrArray;
    }

    @Override
    public void onInitialize() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess,
    environment) -> dispatcher.register(literal("qr")
                .executes(this::qrUnknownCommand)
                .then(literal("help")
                .executes(this::qrHelp))
                .then(argument("link", MessageArgumentType.message())
                .executes(this::runQrCodeGen)
                )));
    }

    private int qrUnknownCommand(CommandContext<ServerCommandSource> context) {
        MutableText text = Text.literal("Unknown command or link! Use /qr help for help.").formatted(Formatting.RED);
        context.getSource().sendFeedback(() -> text, false);
        return 1;
    }

    private int qrHelp(CommandContext<ServerCommandSource> context) {
        MutableText helpText = Text.literal("============ Lazy QR ============\n").formatted(Formatting.DARK_AQUA);
        helpText.append(Text.literal("To generate a qr code, just run the command /qr <link>.\n")).formatted(Formatting.AQUA);
        helpText.append(Text.literal("The qr code always generate the way you are facing, and to the left.\n")).formatted(Formatting.AQUA);
        helpText.append(Text.literal("============ ======= ============")).formatted(Formatting.DARK_AQUA);
        context.getSource().sendFeedback(() -> helpText, false);
        return 1;
    }

    private int runQrCodeGen(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        final String link = MessageArgumentType.getMessage(context, "link").getString();

        if (link == null || link.isEmpty()) {
            context.getSource().sendFeedback(() -> Text.literal("Invalid link!").formatted(Formatting.RED), false);
            return 0;
        }

        final BlockPos playerPos = context.getSource().getPlayer().getBlockPos();
        final ServerWorld world = context.getSource().getWorld();

        final BlockState white_state = Blocks.WHITE_CONCRETE.getDefaultState();
        final BlockState black_state = Blocks.BLACK_CONCRETE.getDefaultState();

        try {
            final boolean[][] qrMatrix = generateQRMatrix(link);
            final Direction facing = context.getSource().getPlayer().getHorizontalFacing();
            int xMultiplier = 1;
            int zMultiplier = 1;

            switch (facing) {
                case NORTH -> {
                    zMultiplier = -1;
                    xMultiplier = -1;
                }
                case WEST -> xMultiplier = -1;
                case EAST -> zMultiplier = -1;
            }

            for (int z = 0; z < qrMatrix.length; z++) {
                final boolean[] row = qrMatrix[z];
                for (int x = 0; x < row.length; x++) {
                    final BlockPos transformedBlockPos = new BlockPos(playerPos.getX() + x * xMultiplier, playerPos.getY()
                            , playerPos.getZ() + z * zMultiplier);
                    world.setBlockState(transformedBlockPos, row[x] ? black_state : white_state);
                }
            }
        } catch (WriterException e) {
            context.getSource().sendFeedback(() -> Text.literal("A error occurred! Please check console!").formatted(Formatting.RED), false);
            LazyQRCodes.LOGGER.error("A exception in generating the QR code has occurred!: ", e);
        }

        return 1;
    }

}