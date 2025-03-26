#!/opt/homebrew/bin/bash

if [ ! -d "lmdb" ]; then
  git clone https://git.openldap.org/openldap/openldap.git lmdb
fi
cd ./lmdb/libraries/liblmdb || exit
git checkout LMDB_0.9.33

declare -A build_outputs
declare -A supported_targets=(
  [iosArm64/liblmdb.dylib]="make CC='xcrun --sdk iphoneos --toolchain iphoneos clang -arch arm64' LDFLAGS='-s' XCFLAGS='-DNDEBUG'"
  [iossimulatorArm64/liblmdb.dylib]="make CC='xcrun --sdk iphonesimulator --toolchain iphoneos clang -arch arm64' LDFLAGS='-s' XCFLAGS='-DNDEBUG'"
  [iossimulatorX64/liblmdb.dylib]="make CC='xcrun --sdk iphonesimulator --toolchain iphoneos clang -arch x86_64' LDFLAGS='-s' XCFLAGS='-DNDEBUG'"
  [linuxArm64/liblmdb.so]="docker run --mount type=bind,source=$(pwd),target=/lmdb --rm --platform=linux/arm64 -w /lmdb gcc:latest make LDFLAGS='-s' XCFLAGS='-DNDEBUG'"
  [linuxX64/liblmdb.so]="docker run --mount type=bind,source=$(pwd),target=/lmdb --rm --platform=linux/amd64 -w /lmdb gcc:latest make LDFLAGS='-s' XCFLAGS='-DNDEBUG'"
  [macosArm64/liblmdb.dylib]="make LDFLAGS='-s' XCFLAGS='-DNDEBUG'"
  [macosX64/liblmdb.dylib]="make CC='clang -mmacosx-version-min=10.15 -arch x86_64' LDFLAGS='-s' XCFLAGS='-DNDEBUG'"
  [mingwX64/lmdb.dll]="make CC='x86_64-w64-mingw32-gcc' AR='x86_64-w64-mingw32-gcc-ar' LDFLAGS='-s' XCFLAGS='-DNDEBUG'"
  [androidNativeArm64/liblmdb.so]="make CC=$NDK/toolchains/llvm/prebuilt/darwin-x86_64/bin/aarch64-linux-android21-clang AR=$NDK/toolchains/llvm/prebuilt/darwin-x86_64/bin/llvm-ar LDFLAGS='-s' XCFLAGS='-UMDB_USE_ROBUST -DMDB_USE_POSIX_MUTEX -DANDROID -DNDEBUG'"
  [androidNativeArm32/liblmdb.so]="make CC=$NDK/toolchains/llvm/prebuilt/darwin-x86_64/bin/armv7a-linux-androideabi21-clang AR=$NDK/toolchains/llvm/prebuilt/darwin-x86_64/bin/llvm-ar LDFLAGS='-s' XCFLAGS='-UMDB_USE_ROBUST -DMDB_USE_POSIX_MUTEX -DANDROID -DNDEBUG'"
  [androidNativeX86/liblmdb.so]="make CC=$NDK/toolchains/llvm/prebuilt/darwin-x86_64/bin/i686-linux-android21-clang AR=$NDK/toolchains/llvm/prebuilt/darwin-x86_64/bin/llvm-ar LDFLAGS='-s' XCFLAGS='-UMDB_USE_ROBUST -DMDB_USE_POSIX_MUTEX -DANDROID -DNDEBUG'"
  [androidNativeX64/liblmdb.so]="make CC=$NDK/toolchains/llvm/prebuilt/darwin-x86_64/bin/x86_64-linux-android21-clang AR=$NDK/toolchains/llvm/prebuilt/darwin-x86_64/bin/llvm-ar LDFLAGS='-s' XCFLAGS='-UMDB_USE_ROBUST -DMDB_USE_POSIX_MUTEX -DANDROID -DNDEBUG'"
  [wasmJs/liblmdb.wasm]="emmake make LDFLAGS='-s' XCFLAGS='-DNDEBUG'"
)

function compile_lib() {
  echo "Build starting for $2"
  make clean
  if ! eval "$1"
  then
    echo "Build failed for $2"
    exit 1
  fi
  echo "Build succeeded for $2"
  output_hash=$(md5 ./liblmdb.so)
  echo "$2 $output_hash"
  build_outputs["$output_hash"]="$2"
  cp ./liblmdb.so ../../../../src/nativeInterop/cinterop/libs/"$2"
  sleep 10 
  #seems to be a stateful race condition on the docker run processes so this allows everything to succeed
}

for key in "${!supported_targets[@]}"; do
  compile_lib "${supported_targets[$key]}" $key
done

if [ ${#supported_targets[@]} -eq ${#build_outputs[@]} ]; then
    echo "All builds for lmdb supported targets have succeeded"
else
    echo "Not all supported targets have produced unique output"
fi