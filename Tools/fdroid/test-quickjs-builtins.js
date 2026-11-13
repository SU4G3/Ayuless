// SPDX-License-Identifier: GPL-3.0-or-later
// Exercise the regenerated builtins, including iterator cleanup and rejections.
function assert(value, message) {
    if (!value) throw new Error(message);
}
function same(actual, expected) {
    assert(JSON.stringify(actual) === JSON.stringify(expected), JSON.stringify(actual));
}
async function testBuiltins() {
    same(Array.from(Iterator.zip([[1, 2], [3, 4]])), [[1, 3], [2, 4]]);
    same(Array.from(Iterator.zip([[1], [2, 3]], {mode: 'longest'})),
         [[1, 2], [undefined, 3]]);
    let closed = false;
    function* values() { try { yield 1; yield 2; } finally { closed = true; } }
    const zipped = Iterator.zip([values(), [3, 4]]);
    zipped.next();
    zipped.return();
    assert(closed, 'zip must close its input on early return');
    let threw = false;
    try { Array.from(Iterator.zip([[1], [2, 3]], {mode: 'strict'})); }
    catch (e) { threw = e instanceof TypeError; }
    assert(threw, 'strict zip must reject unequal lengths');
    same(Array.from(Iterator.zipKeyed({a: [1, 2], b: [3, 4]})),
         [{a: 1, b: 3}, {a: 2, b: 4}]);
    same(await Array.fromAsync([Promise.resolve(1), 2], (x) => x * 2), [2, 4]);
    async function* asyncValues() { yield 3; yield 4; }
    same(await Array.fromAsync(asyncValues(), async (x) => x + 1), [4, 5]);
    threw = false;
    try { await Array.fromAsync([1], () => { throw new Error('mapping failed'); }); }
    catch (e) { threw = e.message === 'mapping failed'; }
    assert(threw, 'fromAsync must propagate mapping errors');
    globalThis.ayulessBuiltinTestsPassed = true;
}
testBuiltins().catch((error) => { globalThis.ayulessBuiltinTestError = error.stack; });
