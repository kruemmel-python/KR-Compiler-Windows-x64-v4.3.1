#include <windows.h>
#include <climits>
#include <cstddef>
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <string>

using AllocateFn = unsigned long long (*)(int, size_t);
using FreeFn = void (*)(int, unsigned long long, size_t);
using WriteFn = void (*)(int, unsigned long long, size_t, const void*);
using QasmFn = int (*)(const char*, void*, int, int*, int*);
using CtcFn = int (*)(const float*, int, int, int, const int*, int,
                      const int*, const int*, int, float*, float*);

template <typename T>
static T symbol(HMODULE library, const char* name) {
    auto value = reinterpret_cast<T>(GetProcAddress(library, name));
    if (!value) std::fprintf(stderr, "Missing export: %s\n", name);
    return value;
}

int main(int argc, char** argv) {
    if (argc != 3) {
        std::fprintf(stderr, "usage: NativeSecurityRegressionTest <dll> <temp-dir>\n");
        return 2;
    }
    HMODULE library = LoadLibraryA(argv[1]);
    if (!library) {
        std::fprintf(stderr, "LoadLibrary failed: %lu\n", GetLastError());
        return 3;
    }
    auto allocate = symbol<AllocateFn>(library, "simulated_kernel_allocate");
    auto release = symbol<FreeFn>(library, "simulated_kernel_free");
    auto write = symbol<WriteFn>(library, "simulated_kernel_write");
    auto importQasm = symbol<QasmFn>(library, "quantum_import_from_qasm");
    auto ctc = symbol<CtcFn>(library, "compute_ctc_loss_cpu");
    if (!allocate || !release || !write || !importQasm || !ctc) return 4;

    unsigned long long handle = allocate(0, 16);
    if (handle == 0) return 5;
    unsigned char data[32]{};
    write(0, handle, 16, data);
    write(0, handle, 32, data);                 // must be rejected
    write(0, 0x4141414141414141ULL, 8, data);   // must be rejected
    release(0, handle, 16);
    release(0, handle, 16);                     // double free must be rejected
    release(0, 0x4242424242424242ULL, 0);       // arbitrary free must be rejected

    std::string qasmPath = std::string(argv[2]) + "\\overlong-token.qasm";
    FILE* qasm = std::fopen(qasmPath.c_str(), "wb");
    if (!qasm) return 6;
    std::string token(200, 'x');
    std::fprintf(qasm, "OPENQASM 2.0;\nqreg q[1];\n%s q[0];\n", token.c_str());
    std::fclose(qasm);
    alignas(16) unsigned char gates[4096]{};
    int gateCount = -1;
    int qubits = -1;
    if (importQasm(qasmPath.c_str(), gates, 8, &gateCount, &qubits) != 0) {
        std::fprintf(stderr, "Overlong QASM token was accepted.\n");
        return 7;
    }

    float logits = 0.0f;
    int target = 0, length = 0, inputLength = 1;
    float loss = 0.0f;
    if (ctc(&logits, 1, 1, 1, &target, INT_MAX, &length, &inputLength, 0, &loss, nullptr) != 0) {
        std::fprintf(stderr, "Overflowing CTC target length was accepted.\n");
        return 8;
    }

    FreeLibrary(library);
    std::puts("Native security regression tests passed.");
    return 0;
}
