package net.kenny.structurespreadtweaks.mixin;

import com.google.gson.JsonElement;
import com.mojang.datafixers.util.Either;
import com.mojang.serialization.Decoder;
import net.kenny.structurespreadtweaks.StructureSpreadTweaksMod;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.packs.resources.Resource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import java.io.Reader;

@Mixin(targets = "net.minecraft.resources.RegistryLoadTask$PendingRegistration")
public final class StructureSetLoadMixin {
    @Inject(
            method = "loadFromResource",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/serialization/Decoder;parse(Lcom/mojang/serialization/DynamicOps;Ljava/lang/Object;)Lcom/mojang/serialization/DataResult;"
            ),
            locals = LocalCapture.CAPTURE_FAILHARD
    )
    private static <T> void villageRarityConfig$patchBeforeDecode(
            Decoder<T> elementDecoder,
            RegistryOps<JsonElement> ops,
            ResourceKey<T> elementKey,
            Resource resource,
            CallbackInfoReturnable<Either<T, Exception>> callback,
            Reader reader,
            JsonElement json
    ) {
        StructureSpreadTweaksMod.patchStructureSetJson(elementKey, json);
    }
}
