import React, { useState } from "react";

export interface User {
  id: number | null;
  name: string;
  phone: string;
}

interface UserFormProps {
  user?: User;
  isOpen: boolean;
  onSave: (user: User) => void;
  onCancel: () => void;
}

const UserForm: React.FC<UserFormProps> = ({ user, isOpen, onSave, onCancel }) => {
  const [name, setName] = useState(user?.name || "");
  const [phone, setPhone] = useState(user?.phone || "");

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onSave({ id: user?.id || null, name, phone });
  };

  return (
    <div
      className={`fixed inset-0 flex items-center justify-center z-50
        transition-opacity duration-300
        ${isOpen ? "opacity-100 pointer-events-auto" : "opacity-0 pointer-events-none"}`}
    >
      {/* backdrop */}
      <div
        className={`absolute inset-0 bg-black/60 transition-opacity duration-300
          ${isOpen ? "opacity-100" : "opacity-0"}`}
        onClick={onCancel}
      />
      {/* modal content */}
      <form
        onSubmit={handleSubmit}
        className={`relative bg-white rounded-lg shadow-lg p-8 w-full max-w-md flex flex-col gap-4
          transform transition-all duration-300 ease-out
          ${isOpen ? "opacity-100 scale-100 translate-y-0" : "opacity-0 scale-95 translate-y-4"}`}
      >
        <h2 className="text-xl font-bold mb-2 text-gray-800">
          {user ? "Sửa người dùng" : "Thêm người dùng"}
        </h2>
        <input
          className="border rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-400"
          type="text"
          placeholder="Tên"
          value={name}
          onChange={e => setName(e.target.value)}
          required
        />
        <input
          className="border rounded px-3 py-2 focus:outline-none focus:ring-2 focus:ring-blue-400"
          type="text"
          placeholder="Số điện thoại"
          value={phone}
          onChange={e => setPhone(e.target.value)}
          required
        />
        <div className="flex gap-2 mt-4">
          <button
            type="submit"
            className="bg-blue-600 text-white px-4 py-2 rounded hover:bg-blue-700 transition"
          >
            Lưu
          </button>
          <button
            type="button"
            className="bg-gray-300 text-gray-700 px-4 py-2 rounded hover:bg-gray-400 transition"
            onClick={onCancel}
          >
            Hủy
          </button>
        </div>
      </form>
    </div>
  );
};

export default UserForm;
