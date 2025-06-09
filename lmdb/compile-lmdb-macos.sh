#!/opt/homebrew/bin/bash

if [ ! -d "lmdb" ]; then
  git clone https://git.openldap.org/openldap/openldap.git lmdb
fi
cd ./lmdb/libraries/liblmdb || exit
git checkout LMDB_0.9.33

declare -A build_outputs
declare -A supported_targets=(
  [linuxArm64/liblmdb.so]="docker run --mount type=bind,source=$(pwd),target=/lmdb --rm --platform=linux/arm64 -w /lmdb gcc:latest make LDFLAGS='-s' XCFLAGS='-DNDEBUG'"
  [linuxX64/liblmdb.so]="docker run --mount type=bind,source=$(pwd),target=/lmdb --rm --platform=linux/amd64 -w /lmdb gcc:latest make LDFLAGS='-s' XCFLAGS='-DNDEBUG'"
  [macosArm64/liblmdb.dylib]="make LDFLAGS='-s' XCFLAGS='-DNDEBUG'"
  [macosX64/liblmdb.dylib]="make CC='clang -mmacosx-version-min=10.15 -arch x86_64' LDFLAGS='-s' XCFLAGS='-DNDEBUG'"
  [mingwX64/lmdb.dll]="make CC='x86_64-w64-mingw32-gcc' AR='x86_64-w64-mingw32-gcc-ar' LDFLAGS='-s' XCFLAGS='-DNDEBUG'"
  [arm64-v8a/liblmdb.so]="make CC=$NDK/toolchains/llvm/prebuilt/darwin-x86_64/bin/aarch64-linux-android21-clang AR=$NDK/toolchains/llvm/prebuilt/darwin-x86_64/bin/llvm-ar LDFLAGS='-s' XCFLAGS='-UMDB_USE_ROBUST -DMDB_USE_POSIX_MUTEX -DANDROID -DNDEBUG'"
  [armeabi-v7a/liblmdb.so]="make CC=$NDK/toolchains/llvm/prebuilt/darwin-x86_64/bin/armv7a-linux-androideabi21-clang AR=$NDK/toolchains/llvm/prebuilt/darwin-x86_64/bin/llvm-ar LDFLAGS='-s' XCFLAGS='-UMDB_USE_ROBUST -DMDB_USE_POSIX_MUTEX -DANDROID -DNDEBUG'"
  [x86/liblmdb.so]="make CC=$NDK/toolchains/llvm/prebuilt/darwin-x86_64/bin/i686-linux-android21-clang AR=$NDK/toolchains/llvm/prebuilt/darwin-x86_64/bin/llvm-ar LDFLAGS='-s' XCFLAGS='-UMDB_USE_ROBUST -DMDB_USE_POSIX_MUTEX -DANDROID -DNDEBUG'"
  [x86_64/liblmdb.so]="make CC=$NDK/toolchains/llvm/prebuilt/darwin-x86_64/bin/x86_64-linux-android21-clang AR=$NDK/toolchains/llvm/prebuilt/darwin-x86_64/bin/llvm-ar LDFLAGS='-s' XCFLAGS='-UMDB_USE_ROBUST -DMDB_USE_POSIX_MUTEX -DANDROID -DNDEBUG'"
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
  # Determine the target directory based on the target
  if [[ "$2" == wasmJs/* ]]; then
    target_dir="../../../../src/wasmWasiMain/resources/"
  elif [[ "$2" == arm64-v8a/* || "$2" == armeabi-v7a/* || "$2" == x86/* || "$2" == x86_64/* ]]; then
    # Android targets go to jniLibs
    target_dir="../../../../src/jvmCommonMain/resources/jniLibs/${2%/*}"
  else
    # JVM targets go to native-libs
    target_dir="../../../../src/jvmCommonMain/resources/native-libs/${2%/*}"
  fi
  
  # The makefile always produces liblmdb.so
  output_file="./liblmdb.so"

  # Calculate hash and copy the file
  output_hash=$(md5 "$output_file")
  echo "$2 $output_hash"
  build_outputs["$output_hash"]="$2"

  # Create target directory if it doesn't exist
  mkdir -p "$target_dir"

  # Copy the file to the appropriate resources directory
  # Extract the target filename from the key (e.g., "liblmdb.dylib" from "macosArm64/liblmdb.dylib")
  target_filename="${2##*/}"
  cp "$output_file" "$target_dir/$target_filename"
  
  # For ES6 module builds, also copy the WASM file
  if [[ "$output_file" == *.mjs ]]; then
    wasm_file="${output_file%.mjs}.wasm"
    if [ -f "$wasm_file" ]; then
      echo "Also copying $wasm_file"
      cp "$wasm_file" "$target_dir"
    fi
  fi

  # Note: With EXPORT_ES6=1, both .mjs and .wasm files are generated
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
